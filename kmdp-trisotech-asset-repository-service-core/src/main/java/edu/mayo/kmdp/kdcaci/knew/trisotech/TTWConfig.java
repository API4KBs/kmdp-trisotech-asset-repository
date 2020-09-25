package edu.mayo.kmdp.kdcaci.knew.trisotech;

import edu.mayo.kmdp.ConfigProperties;
import edu.mayo.kmdp.Opt;
import edu.mayo.kmdp.Option;
import edu.mayo.kmdp.kdcaci.knew.trisotech.TTWConfig.TTWParams;
import edu.mayo.kmdp.terms.skosifier.Modes;
import edu.mayo.kmdp.util.PropertiesUtil;
import java.util.Properties;

public class TTWConfig extends
    ConfigProperties<TTWConfig, TTWParams> {

  private static final Properties DEFAULTS = defaulted( TTWParams.class );

  public TTWConfig() {
    super( DEFAULTS );
  }

  public TTWConfig(Properties defaults) {
    super(defaults);
  }

  @Override
  public TTWParams[] properties() {
    return TTWParams.values();
  }

  public String encode() {
    return PropertiesUtil.serializeProps(this);
  }


  public enum TTWParams implements
      Option<TTWParams> {

    PUBLISHED_ONLY( Opt.of(
        "publishedModelsOnly",
        Boolean.TRUE.toString(),
        "If true, will not expose models unless they are published",
        Boolean.class,
        true) ),

    ;

    private Opt<TTWParams> opt;

    TTWParams( Opt<TTWParams> opt ) {
      this.opt = opt;
    }

    @Override
    public Opt<TTWParams> getOption() {
      return opt;
    }

  }
}