package edu.mayo.kmdp.trisotechwrapper.components.weavers;


import static edu.mayo.kmdp.trisotechwrapper.config.TTConstants.TT_INPUT_BINDINGS;
import static edu.mayo.kmdp.trisotechwrapper.config.TTConstants.TT_METADATA_NS;
import static edu.mayo.kmdp.util.StreamUtil.filterAs;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;

import edu.mayo.kmdp.trisotechwrapper.config.TTConstants;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.util.XPathUtil;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Helper class that reshapes the CMMN/DMN input and output binding contexts.
 * <p>
 * As a precursor of tighter integration, Trisotech uses CMMN extensions to plug in DMN-based
 * mappings between a CMMN decision task and an underlying DMN model's input and output variables.
 * CaseFileItems are used as CMMN's data objects, to pass values as inputs to the decision model,
 * and to collect decision outputs back into case data.
 * <p>
 * The class serves three main functions: (1) removes the use of proprietary extensions, (2)
 * re-distributes the bindings more specifically to the decision task inputs and outputs, (3)
 * normalizes the schemas across versions
 */
public class DataBindingManipulator {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(DataBindingManipulator.class);

  /**
   * Inaccessible constructor - This class only exposes public static functions
   */
  private DataBindingManipulator() {
    // nothing to do
  }

  /**
   * Rewrites each one of the CMMN input bindings to avoid the use of proprietary extensions
   * <p>
   * 6/22 The (DMN-style) Definition of a CMMN Task can have 0..* formal Inputs - named and typed
   * parameters with a (S)DMN datatype definition. It is a responsibility of the CMMN model to
   * provide actual values for the variables, usually by means of 'datatype' cmmn:CaseFileItems.
   * <p>
   * This method rewrites the bindings between the formal and actual parameters, to avoid using
   * proprietary extensions. Assumes that the binding is either missing, or formalized as a
   * dmn:Context, and the context contains one entry per Input
   *
   * @param dox The CMMN model to rewrite, as an XML DOM
   */
  public static void rewriteInputDataBindings(
      @Nonnull final Document dox) {
    // the triso:InputBindings are the proprietary elements
    asElementStream(dox.getElementsByTagNameNS(
        TT_METADATA_NS, TT_INPUT_BINDINGS))
        // which contain the standard dmn:Context bindings
        // "standard" in the semi-standard sense of using DMN inside CMMN
        .forEach(trisoInputBinding -> findExogenousDMNElement(trisoInputBinding)
            // remap the dmn:Context with the bindings to the cmmn:Inputs
            .ifPresent(dmnBindings ->
                rewriteInputDataBindingContext(trisoInputBinding, dmnBindings, dox)));
  }


  /**
   * Rewrites each one of the CMMN output bindings to avoid the use of proprietary extensions
   * <p>
   * 6/22 The (DMN-style) Definition of a CMMN Task can have 0..* formal Outputs - named and typed
   * parameters with a (S)DMN datatype definition. Decision Tasks that reference a specific Decision
   * have one (possibly structured) formal Output. Decision Tasks that reference a Decision Service
   * can have 1+ formal Outputs
   * <p>
   * This method rewrites the bindings between the formal and actual parameters, to avoid using
   * proprietary extensions. Assumes that the binding is either missing, or formalized as a
   * dmn:Context, and the context contains one entry per Output
   *
   * @param dox The CMMN model to rewrite, as an XML DOM
   */
  public static void rewriteOutputDataBindings(
      @Nonnull final Document dox) {
    // the triso:OutputBindings are the proprietary elements
    asElementStream(dox.getElementsByTagNameNS(
        TT_METADATA_NS, TTConstants.TT_OUTPUT_BINDINGS))
        // which contain the standard dmn:Context bindings
        // "standard" in the semi-standard sense of using DMN inside CMMN
        .forEach(trisoOutputBinding -> findExogenousDMNElement(trisoOutputBinding)
            // remap the dmn:Context with the bindings to the cmmn:Inputs
            .ifPresent(dmnBindings ->
                rewriteOutputDataBindingContext(trisoOutputBinding, dmnBindings, dox)));
  }


  /**
   * Rewrites the Input Data Mappings associated to a specific CMMN Task, to avoid the use of
   * proprietary extensions. Distributes the mapping context across the specific Task inputs.
   * <p>
   * Example: Assume that Task T has formal inputs A and B. The Task will wrap a dmn:Context with
   * entries {A = ..., B = ...}, such that the expressions in the Context determine how to derive
   * the actual parameters values at runtime. This method splits the Context into one sub-context
   * per entry, e.g. {A = ...} and {B = ...}, and remaps each sub-context to the specific input.
   *
   * @param trisoInputBinding The proprietary extension element
   * @param dmnBindings       The DMN Context that maps the CMMN Task input variables (actual
   *                          parameters) to the Task model Inputs (formal parameters)
   * @param dox               the CMMN Model to rewrite (as an XML DOM)
   */
  public static void rewriteInputDataBindingContext(
      @Nonnull final Element trisoInputBinding,
      @Nonnull final Element dmnBindings,
      @Nonnull final Document dox) {
    XPathUtil x = new XPathUtil();
    // Each triso:InputBinding is set as a cmmn:extensionElement of its cmmn:Task
    Element scopingTask = (Element) trisoInputBinding.getParentNode().getParentNode();

    //TODO: 6/20/22 - the exporter associates the namespace prefix "dmn" to DMN 1.3,
    // but the platform is on DMN 1.2
    XMLUtil.migrateNamespace(dox, dmnBindings, TTConstants.DMN_13_XMLNS, TTConstants.DMN_12_XMLNS);

    // Finds the context entries by Variable, then remaps each one
    asElementStream(
        x.xList(dmnBindings, ".//dmn:variable"))
        .forEach(varNode -> rewriteInputDataBinding(scopingTask, dmnBindings, varNode, dox, x));

    // removes the original bindings
    trisoInputBinding.getParentNode().removeChild(trisoInputBinding);
  }

  /**
   * Rewrites an Input binding as follows: looks up the input reference; extracts the context entry
   * which assigns the input value to the input; reassigns the context entry to the Input; if the
   * input value is mapped from a CFI, the input will be 'boundRef' to that CFI
   *
   * <pre>
   * {@code
   *   <cmmn:caseFileItem id="CFI_ID" name="(CFI)" />
   *
   *   <triso:dataInputBindings>
   *     <dmn:context>
   *       <dmn:contextEntry>
   *         <dmn:literalExpression>(CFI)</dmn:literalExpression>
   *         <dmn:variable name="(DMN) Input Name" triso:inputRef="INPUT_ID"/>
   *       </dmn:contextEntry>
   *     </dmn:context>
   *   </triso:dataInputBindings>
   *   <!-- will be injected into -->
   *   <cmmn:input id="INPUT_ID" name="(DMN) Input Name" bindingRef="CFI_ID" />
   * }
   * </pre>
   *
   * @param scopingTask The CMMN Task whose Input bindings are being rewritten
   * @param dmnContext  The DMN mapping between input variables and binding expressions
   * @param varNode     The dmn:variable node, whose variable name references the DMN Task Input,
   *                    and maps to the binding expression in the dmn:Context
   * @param dox         The scoping CMM Model, as an XML document
   * @param x           The XPath helper
   */
  protected static void rewriteInputDataBinding(
      @Nonnull final Element scopingTask,
      @Nonnull final Element dmnContext,
      @Nonnull final Element varNode,
      @Nonnull final Document dox,
      @Nonnull final XPathUtil x) {
    // Resolve the reference from the context entry's variable to the task input
    Optional<String> inputRef =
        Optional.of(varNode.getAttribute("triso:inputRef"))
            .filter(Util::isNotEmpty)
            .or(() -> Optional.of(varNode.getAttribute("name")))
            .filter(Util::isNotEmpty);

    if (inputRef.isPresent()) {
      String ref = inputRef.get();
      // looks up the Task input referenced by the DMN context
      Optional<Node> input = Optional.ofNullable(
          x.xNode(scopingTask,
              "../..//cmmn:input[@id='" + ref + "']"));
      // when a context entry maps to a task input, rewrite
      if (input.isPresent()) {
        var inputNode = (Element) input.get();

        // clones the original binding context, preserving the bindings for the input
        Element bindings = getScopedBindings(dmnContext,
            entry -> x.xNode(entry,
                ".//dmn:variable[@id='" + varNode.getAttribute("id") + "']") != null);

        // remaps the bindings to the input
        asElementStream(bindings.getChildNodes())
            .forEach(entry -> processInputContextEntry(entry, inputNode, dox, x));

        // adds the rewritten context entry to the task input
        // note: CFIs may or may not be involved in an input mapping
        addExtension(inputNode, bindings, dox, x);
      }
    }
  }

  /**
   * Adds extensionElement as a child of (the extensions of) scopingElement, within Document dox.
   * <p>
   * Ensures that scopingElement extension collection is present, or creates an empty one, before
   * adding the extensionElement child
   *
   * @param scopingElement   The parent element to be extended
   * @param extensionElement The child element to be added as an extension of its parent
   * @param dox              The scoping Document
   * @param x                The XPath helper
   */
  private static void addExtension(
      @Nonnull final Element scopingElement,
      @Nonnull final Element extensionElement,
      @Nonnull final Document dox,
      @Nonnull final XPathUtil x) {
    // ensure an 'extensions' element is present
    Element extensions = ensureCMMNExtensions(dox, scopingElement, x);
    extensions.appendChild(extensionElement);
  }

  /**
   * Creates a filtered clone of a binding dmn:Context, filtering the entries in the process
   *
   * @param dmnContext The context
   * @param filter     a Predicate that applies to dmn:contextEntry nodes. If the predicate
   *                   succeeds, the entry node will be retained
   * @return a cloned copy of the input context, with only the entries that match the filter
   */
  @Nonnull
  private static Element getScopedBindings(
      @Nonnull final Element dmnContext,
      @Nonnull final Predicate<Element> filter) {
    var cloned = (Element) dmnContext.cloneNode(true);
    asElementStream(cloned.getChildNodes())
        .filter(el -> !filter.test(el))
        .forEach(cloned::removeChild);
    return cloned;
  }

  /**
   * Compares a DMN input context entry to a CMMN decision task input. If matching, will rewrite the
   * context entry as an extension of the task input
   * <p>
   * Note: this method is called once per (DMN) input x N for each entry in the shared input
   * Context
   *
   * @param contextEntry each entry in the inputBinding context - originally shared across all
   *                     inputs, but at this point cloned for a specific input
   * @param inputEl      the decision task input element to match, driven by the underlying DMN
   *                     input
   * @param dox          the scoping model
   * @param x            the XPath processor
   */
  private static void processInputContextEntry(
      @Nonnull final Element contextEntry,
      @Nonnull final Element inputEl,
      @Nonnull final Document dox,
      @Nonnull final XPathUtil x) {
    // this is a clone of the shared inputBindings, specific to the inputNode
    Element bindings = (Element) contextEntry.getParentNode();

    // check if the entry references one specific CFI, and link it
    Optional.of(x.xString(bindings, ".//dmn:literalExpression/dmn:text/text()"))
        .filter(Util::isNotEmpty)
        .map(val -> (Element) x.xNode(dox, "//cmmn:caseFileItem[@name='" + val + "']"))
        .ifPresent(cfi -> inputEl.setAttribute("bindingRef", cfi.getAttribute("id")));
  }

  /**
   * Looks up a DMN - to become SDMN - data binding, associated to a cmmn:Task input/output binding
   *
   * @param bindingElement The cmmn input/output binding
   * @return the DMN context that defines the I/O variable mappings
   */
  @Nonnull
  protected static Optional<Element> findExogenousDMNElement(
      @Nonnull final Element bindingElement) {
    return asElementStream(bindingElement.getChildNodes())
        .flatMap(filterAs(Element.class))
        .filter(el -> TTConstants.DMN_12_XMLNS.equals(el.getNamespaceURI())
            || TTConstants.DMN_13_XMLNS.equals(el.getNamespaceURI())
            || TTConstants.DMN_14_XMLNS.equals(el.getNamespaceURI()))
        .findFirst();
  }

  /**
   * Ensures that a rootNode from a CMMN model has an 'extensionElements' child, adding it if not.
   *
   * @param dox      The root CMMN document
   * @param rootNode The node that has, or will have, a cmmn:extensionElements child Element
   * @return the extensionElements node
   */
  @Nonnull
  protected static Element ensureCMMNExtensions(
      @Nonnull final Document dox,
      @Nonnull final Node rootNode,
      @Nonnull final XPathUtil x) {
    Element extensions = (Element) x.xNode(rootNode, "./cmmn:extensionElements");
    if (extensions == null) {
      extensions = dox.createElementNS(TTConstants.CMMN_11_XMLNS, TTConstants.CMMN_EL_EXTENSIONS);
      rootNode.appendChild(extensions);
    }
    return extensions;
  }


  /**
   * Rewrites the Output Data Mappings associated to a specific CMMN Task, to avoid the use of
   * proprietary extensions. Distributes the mapping context across the specific Task outputs.
   * <p>
   *
   * @param trisoOutputBinding The proprietary extension element
   * @param dmnBindings        The DMN Context that maps the CMMN Task input variables (actual
   *                           parameters) to the Task model Inputs (formal parameters)
   * @param dox                the CMMN Model to rewrite (as an XML DOM)
   * @see this.rewriteInputDataBindingContext
   */
  protected static void rewriteOutputDataBindingContext(
      @Nonnull final Element trisoOutputBinding,
      @Nonnull final Element dmnBindings,
      @Nonnull final Document dox) {
    XPathUtil x = new XPathUtil();
    // Each triso:OutputBinding is set as a cmmn:extensionElement of its cmmn:Task
    Element scopingTask = (Element) trisoOutputBinding.getParentNode().getParentNode();

    //TODO: 6/20/22 - the exporter associates the namespace prefix "dmn" to DMN 1.3,
    // but the platform is on DMN 1.2
    XMLUtil.migrateNamespace(dox, dmnBindings, TTConstants.DMN_13_XMLNS, TTConstants.DMN_12_XMLNS);

    // Finds the context entries by Variable, then remaps each one
    asElementStream(
        x.xList(dmnBindings, ".//dmn:variable"))
        .forEach(varNode -> rewriteOutputDataBinding(scopingTask, dmnBindings, varNode, dox, x));

    // removes the original bindings
    trisoOutputBinding.getParentNode().removeChild(trisoOutputBinding);
  }


  /**
   * Rewrites an Output binding as follows: looks up the referenced CFI, if any. At this point,
   * output bindings either target a CFI, or do not have a binding. If a CFI is identified, matches
   * the expression that assigns the value to the CFI to the corresponding (DMN) decision output.
   * Reassigns the context entry to the Output, and 'boundRefs' the output to that CFI.
   * <p>
   * Not ideal: if the same DMN output maps to 2+ CFI, the output is cloned, because of the schema
   * limitation: one Output can be boundRef to 0..1 CFIs, but not many.
   *
   * <pre>
   * {@code
   *   <cmmn:caseFileItem id="CFI_ID" name="(CFI)" />
   *
   *   <triso:dataOutputBindings>
   *     <dmn:context>
   *       <dmn:contextEntry>
   *         <dmn:literalExpression>DMN Output Name</dmn:literalExpression>
   *         <dmn:variable name="(CFI)" triso:caseFileItemRef="CFI_ID/>
   *       </dmn:contextEntry>
   *     </dmn:context>
   *   </triso:dataOutputBindings>
   *
   *   <!-- will be injected into, adding 'bindingRef="CFI_ID"' -->
   *   <cmmn:output id="OUTPUT_ID" name="DMN Output Name" />
   * }
   *
   * @param scopingTask The CMMN Task whose Output bindings are being rewritten
   * @param dmnContext  The DMN mapping between output variables and binding expressions
   * @param varNode     The dmn:variable node, whose variable name references the DMN Task Output,
   *                    and maps to the binding expression in the dmn:Context
   * @param dox         The scoping CMM Model, as an XML document
   * @param x           The XPath helper
   */
  protected static void rewriteOutputDataBinding(
      @Nonnull final Element scopingTask,
      @Nonnull final Element dmnContext,
      @Nonnull final Element varNode,
      @Nonnull final Document dox,
      @Nonnull final XPathUtil x) {

    // look up the referenced CFI
    var cfi = Optional.of(
            varNode.getAttributeNS(TT_METADATA_NS, "caseFileItemRef"))
        .map(ref -> x.xNode(dox, "//cmmn:caseFileItem[@id='" + ref + "']"));

    if (cfi.isEmpty()) {
      logger.error(
          "Unable to resolve referenced CaseFileItem for output var {} binding in task {}",
          varNode.getAttribute("name"), scopingTask.getAttribute("name"));
      return;
    }

    // look up the binding expression - expected to be a plain reference to a CFI
    Optional<String> expr = Optional.ofNullable
            (x.xString(varNode, "../dmn:literalExpression/dmn:text"))
        .map(String::trim)
        .filter(Util::isNotEmpty);

    // looks up the Task output for the CFI reference
    Optional<Node> output = expr.map(cfiName ->
        x.xNode(scopingTask,
            "../..//cmmn:output[@name='" + cfiName + "']"));

    // when a context entry maps to a task output, rewrite
    if (output.isPresent()) {
      var cfiEl = (Element) cfi.get();
      var outputEl = (Element) output.get();

      // clones the context and removes the NON-matching entries
      // as opposed to extracting just the matching entries
      // in case the context container contains additional elements
      Element bindings = getScopedBindings(dmnContext,
          entry -> x.xNode(entry,
              ".//dmn:variable[@name='" + cfiEl.getAttribute("name") + "']") != null);

      // apply the bindings to the output
      asElementStream(bindings.getChildNodes())
          .forEach(
              contextEntry -> processOutputContextEntry(contextEntry, outputEl, cfiEl, dox, x));
    }
  }

  /**
   * Applies the CFI binding to a decision task output node.
   * <p>
   * If the output is already bound to a CFI, will clone the output (this scenario is undesirable,
   * but possible)
   *
   * @param contextEntry the context entry that bounds the decision output to the CFI
   * @param outputEl     the decision task output
   * @param cfiEl        the bound CaseFileItem
   * @param dox          the scoping model
   * @param x            the XPath processor
   */
  private static void processOutputContextEntry(
      @Nonnull final Element contextEntry,
      @Nonnull final Element outputEl,
      @Nonnull final Element cfiEl,
      @Nonnull final Document dox,
      @Nonnull final XPathUtil x) {
    boolean isUnbound = Util.isEmpty(outputEl.getAttribute("bindingRef"));

    if (isUnbound) {
      processUnboundOutputContextEntry(contextEntry, outputEl, cfiEl, dox, x);
    } else {
      if (logger.isWarnEnabled()) {
        logger.warn("Detected 1:N mapping of DMN output '{}' to CMMN CaseFileItems",
            outputEl.getAttribute("name"));
      }
      processBoundOutputContextEntry(contextEntry, outputEl, cfiEl, dox, x);
    }
  }


  /**
   * Applies the CFI binding to a decision task output node.
   * <p>
   * Adds a reference to the CFI, and sets the dmn context as a child extension
   *
   * @param contextEntry the context entry that bounds the decision output to the CFI
   * @param outputEl     the decision task output
   * @param cfiEl        the bound CaseFileItem
   * @param dox          the scoping model
   * @param x            the XPath processor
   */
  private static void processUnboundOutputContextEntry(
      @Nonnull final Element contextEntry,
      @Nonnull final Element outputEl,
      @Nonnull final Element cfiEl,
      @Nonnull final Document dox,
      @Nonnull final XPathUtil x) {
    String cfiRef = cfiEl.getAttribute("id");
    Element bindings = (Element) contextEntry.getParentNode();

    outputEl.setAttribute("bindingRef", cfiRef);
    // adds the rewritten context entry to the task input
    addExtension(outputEl, bindings, dox, x);
  }

  /**
   * Applies the CFI binding to a decision task output node, when the output is already bound.
   * Clones the output and removes the binding, so that the clone can be bound to the additional
   * context.
   *
   * @param contextEntry the context entry that bounds the decision output to the CFI
   * @param outputEl     the decision task output
   * @param cfiEl        the bound CaseFileItem
   * @param dox          the scoping model
   * @param x            the XPath processor
   */
  private static void processBoundOutputContextEntry(
      @Nonnull final Element contextEntry,
      @Nonnull final Element outputEl,
      @Nonnull final Element cfiEl,
      @Nonnull final Document dox,
      @Nonnull final XPathUtil x) {

    var clonedOut = (Element) outputEl.cloneNode(true);
    clonedOut.setAttribute("id", "_" + UUID.randomUUID());
    clonedOut.removeAttribute("bindingRef");

    Element extensions = ensureCMMNExtensions(dox, clonedOut, x);
    asElementStream(extensions.getChildNodes())
        .filter(n -> n.getNodeName().equals("context"))
        .forEach(extensions::removeChild);

    outputEl.getParentNode().appendChild(clonedOut);

    processUnboundOutputContextEntry(contextEntry, clonedOut, cfiEl, dox, x);
  }
}

