package edu.mayo.kmdp.kdcaci.knew.trisotech.components;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.html.HtmlDeserializer;
import edu.mayo.kmdp.util.ws.HTMLKnowledgeCarrierWrapper;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.annotation.Nonnull;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Custom converter that unwraps HTML content for end-user agent clients
 * <p>
 * Extends the generic HTMLKnowledgeCarrierWrapper to use a HTMLDeserializer
 * <p>
 * Background: most API4KP operations wrap/tunnel Knowledge Models in a {@link KnowledgeCarrier}, to
 * enable uniform access and pipeline integration. IF the client is a user-agent (e.g. browser) AND
 * the client is asking for text/html content, AND the carrier contains an HTML artifact, THEN it is
 * assumed that the final use is presentation (as opposed to further processing), and the content
 * can be unwrapped from the KnowledgeCarrier.
 */
public class HTMLAdapter extends HTMLKnowledgeCarrierWrapper {

  @Override
  public void write(
      @Nonnull final KnowledgeCarrier kc,
      final MediaType contentType,
      @Nonnull final HttpOutputMessage outputMessage)
      throws IOException, HttpMessageNotWritableException {
    byte[] html = new HtmlDeserializer()
        .applyLower(kc, Encoded_Knowledge_Expression,
            codedRep(HTML, TXT, Charset.defaultCharset(), Encodings.DEFAULT), null)
        .flatOpt(AbstractCarrier::asBinary)
        .orElseThrow(() -> new HttpMessageNotWritableException("Unable to write HTML"));
    outputMessage.getBody().write(html);
  }

}
