package ocss.nmea.parser;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

public class StringGenerator
{
  private static final SimpleDateFormat SDF_TIME = new SimpleDateFormat("HHmmss");
  private static final SimpleDateFormat SDF_DATE = new SimpleDateFormat("ddMMyy");
  private final static NumberFormat LAT_DEG_FMT = new DecimalFormat("00");
  private final static NumberFormat LONG_DEG_FMT = new DecimalFormat("000");
  private final static NumberFormat MIN_FMT = new DecimalFormat("00.000");
  private final static NumberFormat OG_FMT = new DecimalFormat("000.0");
  private final static NumberFormat TEMP_FMT = new DecimalFormat("#0.0");
  private final static NumberFormat PRMSL_FMT = new DecimalFormat("##0.0000");
  private final static NumberFormat PRMSL_FMT_2 = new DecimalFormat("##0");
  private final static NumberFormat PERCENT_FMT = new DecimalFormat("##0");
  private final static NumberFormat DIR_FMT = new DecimalFormat("##0");
  private final static NumberFormat SPEED_FMT = new DecimalFormat("#0.0");
/*
 * Common talker IDs
  |================================================================
  |GP    |  Global Positioning System receiver
  |LC    |  Loran-C receiver
  |II    |  Integrated Instrumentation
  |IN    |  Integrated Navigation
  |EC    |  Electronic Chart Display & Information System (ECDIS)
  |CD    |  Digital Selective Calling (DSC)
  |GL    |  GLONASS, according to IEIC 61162-1
  |GN    |  Mixed GPS and GLONASS data, according to IEIC 61162-1
  |================================================================
 */
  
  /*
   * XDR - Transducer Measurements
      $--XDR,a,x.x,a,c--c,...����...a,x.x,a,c--c*hh<CR><LF>
             | |   | |    |        ||     |
             | |   | |    |        |+-----+-- Transducer 'n'
             | |   | |    +--------+- Data for variable # of transducers
             | |   | +- Transducer #1 ID
             | |   +- Units of measure, Transducer #1
             | +- Measurement data, Transducer #1
             +- Transducer type, Transducer #1
      Notes:
      1) Sets of the four fields 'Type-Data-Units-ID' are allowed for an undefined number of transducers.
      Up to 'n' transducers may be included within the limits of allowed sentence length, null fields are not
      required except where portions of the 'Type-Data-Units-ID' combination are not available.
      2) Allowed transducer types and their units of measure are:
      Transducer           Type Field  Units Field              Comments
      -------------------------------------------------------------------
      temperature            C           C = degrees Celsius
      angular displacement   A           D = degrees            "-" = anti-clockwise
      linear displacement    D           M = meters             "-" = compression
      frequency              F           H = Hertz
      force                  N           N = Newton             "-" = compression
      pressure               P           B = Bars, P = Pascal   "-" = vacuum
      flow rate              R           l = liters/second
      tachometer             T           R = RPM
      humidity               H           P = Percent
      volume                 V           M = cubic meters
      generic                G           none (null)            x.x = variable data
      current                I           A = Amperes
      voltage                U           V = Volts
      switch or valve        S           none (null)            1 = ON/ CLOSED, 0 = OFF/ OPEN
      salinity               L           S = ppt                ppt = parts per thousand
   */
  
  public static enum XDRTypes // Se above for more details
  {
    TEMPERATURE         ("C", "C"), // in Celcius
    ANGULAR_DISPLACEMENT("A", "D"), // In degrees
    LINEAR_DISPLACEMENT ("D", "M"), // In meters
    FREQUENCY           ("F", "H"), // In Hertz
    FORCE               ("N", "N"), // In Newtons
    PRESSURE_B          ("P", "B"), // In Bars
    PRESSURE_P          ("P", "P"), // In Pascals
    FLOW_RATE           ("R", "l"), // In liters
    TACHOMETER          ("T", "R"), // In RPM
    HUMIDITY            ("H", "P"), // In %
    VOLUME              ("V", "M"), // In Cubic meters
    GENERIC             ("G", ""),  // No unit
    CURRENT             ("I", "A"), // In Amperes
    VOLTAGE             ("U", "V"), // In Volts
    SWITCH_OR_VALVE     ("S", ""),  // No Unit
    SALINITY            ("L", "S"); // In Parts per Thousand
    
    private final String type;
    private final String unit;

    XDRTypes(String type, String unit)
    {
      this.type = type;
      this.unit = unit;
    }
    
    public String type() { return this.type; }
    public String unit() { return this.unit; }
  };
  
  public static class XDRElement
  {
    private XDRTypes typeNunit;
    private double value;
    private String transducerName;
    
    public XDRElement(XDRTypes tnu, double value, String tdName)
    {
      this.typeNunit = tnu;
      this.value = value;
      this.transducerName = tdName;
    }

    public StringGenerator.XDRTypes getTypeNunit()
    {
      return typeNunit;
    }

    public double getValue()
    {
      return value;
    }
    
    public String getTransducerName()
    {
      return this.transducerName;
    }
    
    public String toString()
    {
      return this.transducerName + ", " + this.getTypeNunit() + ", " +this.getTypeNunit().type() + ", " + Double.toString(this.getValue()) + " " + this.getTypeNunit().unit();
    }
  }
  
  public static String generateXDR(String devicePrefix, XDRElement first, XDRElement... next) 
  {
    String xdr = devicePrefix + "XDR,";
    xdr += (first.getTypeNunit().type() + ",");
    xdr += (Double.toString(first.getValue()) + ",");
    xdr += (first.getTypeNunit().unit() + ",");
    xdr += (first.getTransducerName());
    
    for (XDRElement e : next)
    {
      NumberFormat nf = null;
      // TASK More formats
      if (e.getTypeNunit().type().equals(XDRTypes.PRESSURE_B))
        nf = PRMSL_FMT;
      if (e.getTypeNunit().type().equals(XDRTypes.PRESSURE_P))
        nf = PRMSL_FMT_2;
      if (e.getTypeNunit().type().equals(XDRTypes.TEMPERATURE))
        nf = TEMP_FMT;
      xdr += ("," + e.getTypeNunit().type() + ",");
      if (nf != null)
        xdr += (nf.format(e.getValue()) + ",");
      else
        xdr += (Double.toString(e.getValue()) + ",");
      xdr += (e.getTypeNunit().unit() + ",");
      xdr += (e.getTransducerName());
    }
    // Checksum
    int cs = StringParsers.calculateCheckSum(xdr);
    xdr += ("*" + lpad(Integer.toString(cs, 16).toUpperCase(), "0", 2));
    
    return "$" + xdr;
  }  

  /*  
  $--MDA,x.x,I,x.x,B,x.x,C,x.x,C,x.x,x.x,x.x,C,x.x,T,x.x,M,x.x,N,x.x,M*hh<CR><LF> 
         |   | |   | |   | |   | |   |   |   | |   | |   | |   | |   |
         |   | |   | |   | |   | |   |   |   | |   | |   | |   | +---+- Wind Speed, m/s
         |   | |   | |   | |   | |   |   |   | |   | |   | +---+- Wind Speed, Knots
         |   | |   | |   | |   | |   |   |   | |   | +---+- Wind Dir, Magnetic
         |   | |   | |   | |   | |   |   |   | +---+- Wind Dir, True
         |   | |   | |   | |   | |   |   +---+- Dew point, degrees C 
         |   | |   | |   | |   | |   +- Absolute humidity, percent 
         |   | |   | |   | |   | +- Relative humidity, percent 
         |   | |   | |   | +---+- Water temperature, degrees C 
         |   | |   | +---+- Air temperature, degrees C 
         |   | +---+- Barometric pressure, bars 
         +---+- Barometric pressure, inches of mercury 
  */
  public static String generateMDA(String devicePrefix, double pressureInhPa, // ~ mb
                                                        double airTempInDegrees,
                                                        double waterTempInDegrees,
                                                        double relHumidity,
                                                        double absHumidity,
                                                        double dewPointInCelcius,
                                                        double windDirTrue,
                                                        double windDirMag,
                                                        double windSpeedInKnots)
  {
    String mda = devicePrefix + "MDA,";
    mda+= (PRMSL_FMT.format(pressureInhPa / Pressure.HPA_TO_INHG) + ",I,");
    mda+= (PRMSL_FMT.format(pressureInhPa / 1000) + ",B,");
    mda+= (TEMP_FMT.format(airTempInDegrees) + ",C,");
    mda+= (TEMP_FMT.format(waterTempInDegrees) + ",C,");
    mda+= (PERCENT_FMT.format(relHumidity) + ",");
    mda+= (PERCENT_FMT.format(absHumidity) + ",");
    mda+= (DIR_FMT.format(dewPointInCelcius) + ",C,");
    mda+= (TEMP_FMT.format(windDirTrue) + ",T,");
    mda+= (TEMP_FMT.format(windDirMag) + ",M,");
    mda+= (SPEED_FMT.format(windSpeedInKnots) + ",N,");
    mda+= (SPEED_FMT.format(windSpeedInKnots * 1.852 / 3.6) + ",M");
    int cs = StringParsers.calculateCheckSum(mda);
    mda += ("*" + lpad(Integer.toString(cs, 16).toUpperCase(), "0", 2));
    
    return "$" + mda;
  }
  
  /*
   * Barometric pressure
   */
  public static String generateMMB(String devicePrefix, double mbPressure) // pressure in mb
  {
    String mmb = devicePrefix + "MMB,";
    mmb += (PRMSL_FMT.format(mbPressure / 33.8600) + ",I,"); // Inches of Hg
    mmb += (PRMSL_FMT.format(mbPressure / 1000) + ",B");     // Bars. 1 mb = 1 hPa
    // Checksum
    int cs = StringParsers.calculateCheckSum(mmb);
    mmb += ("*" + lpad(Integer.toString(cs, 16).toUpperCase(), "0", 2));
    
    return "$" + mmb;
  }
  
  /*
   * Air temperature
   */
  public static String generateMTA(String devicePrefix, double temperature) // in Celcius
  {
    String mta = devicePrefix + "MTA,";
    mta += (TEMP_FMT.format(temperature) + ",C");
    // Checksum
    int cs = StringParsers.calculateCheckSum(mta);
    mta += ("*" + lpad(Integer.toString(cs, 16).toUpperCase(), "0", 2));
    
    return "$" + mta;
  }
  
  public static String generateRMC(String devicePrefix, Date date, double lat, double lng, double sog, double cog, double d)
  {
    String rmc = devicePrefix + "RMC,";
    rmc += (SDF_TIME.format(date) + ",");
    rmc += "A,";
    int deg = (int)Math.abs(lat);
    double min = 0.6 * ((Math.abs(lat) - deg) * 100d);
    rmc += (LAT_DEG_FMT.format(deg) + MIN_FMT.format(min));
    if (lat < 0) rmc += ",S,";
    else rmc += ",N,";

    deg = (int)Math.abs(lng);
    min = 0.6 * ((Math.abs(lng) - deg) * 100d);
    rmc += (LONG_DEG_FMT.format(deg) + MIN_FMT.format(min));
    if (lng < 0) rmc += ",W,";
    else rmc += ",E,";
    
    rmc += (OG_FMT.format(sog) + ",");
    rmc += (OG_FMT.format(cog) + ",");

    rmc += (SDF_DATE.format(date) + ",");

    rmc += (OG_FMT.format(Math.abs(d)) + ",");
    if (d < 0) rmc += "W";
    else rmc += "E";    
    // Checksum
    int cs = StringParsers.calculateCheckSum(rmc);
    rmc += ("*" + lpad(Integer.toString(cs, 16).toUpperCase(), "0", 2));
    
    return "$" + rmc;
  }
  
  public static String generateMWV(String devicePrefix, double aws, int awa)
  {
    String mwv = devicePrefix + "MWV,";
    mwv += (OG_FMT.format(awa) + ",R,");
    mwv += (OG_FMT.format(aws) + ",N,A");
    // Checksum
    int cs = StringParsers.calculateCheckSum(mwv);
    mwv += ("*" + lpad(Integer.toString(cs, 16).toUpperCase(), "0", 2));
    
    return "$" + mwv;
  }
  
  public static String generateVHW(String devicePrefix, double bsp, int cc)
  {
    String vhw = devicePrefix + "VHW,,,";
    vhw += (LONG_DEG_FMT.format(cc) + ",M,");
    vhw += (MIN_FMT.format(bsp) + ",N,,");
    // Checksum
    int cs = StringParsers.calculateCheckSum(vhw);
    vhw += ("*" + lpad(Integer.toString(cs, 16).toUpperCase(), "0", 2));
    
    return "$" + vhw;
  }
  
  public static String generateHDM(String devicePrefix, int cc)
  {
    String hdm = devicePrefix + "HDM,";
    hdm += (LONG_DEG_FMT.format(cc) + ",M");
    // Checksum
    int cs = StringParsers.calculateCheckSum(hdm);
    hdm += ("*" + lpad(Integer.toString(cs, 16).toUpperCase(), "0", 2));
    
    return "$" + hdm;
  }
  
  private static String lpad(String s, String pad, int len)
  {
    String padded = s;
    while (padded.length() < len)
      padded = pad + padded;
    return padded;
  }
  
  public static void main(String[] args)
  {
    String rmc = generateRMC("II", new Date(), 38.2500, -122.5, 6.7, 210, 3d);
    System.out.println("Generated RMC:" + rmc);
    
    if (StringParsers.validCheckSum(rmc))
      System.out.println("Valid!");
    else
      System.out.println("Invalid...");
    
    String mwv = generateMWV("II", 23.45, 110);
    System.out.println("Generated MWV:" + mwv);
    
    if (StringParsers.validCheckSum(mwv))
      System.out.println("Valid!");
    else
      System.out.println("Invalid...");

    String vhw = generateVHW("II", 8.5, 110);
    System.out.println("Generated VHW:" + vhw);
    
    if (StringParsers.validCheckSum(vhw))
      System.out.println("Valid!");
    else
      System.out.println("Invalid...");
    
    String mmb = generateMMB("II", 1013.6);
    System.out.println("Generated MMB:" + mmb);
    
    String mta = generateMTA("II", 20.5);
    System.out.println("Generated MTA:" + mta);
    
    String xdr = generateXDR("II", new XDRElement(XDRTypes.PRESSURE_B, 1.0136, "BMP180"));
    System.out.println("Generated XDR:" + xdr);
    xdr = generateXDR("II", new XDRElement(XDRTypes.PRESSURE_B, 1.0136, "BMP180"), new XDRElement(XDRTypes.TEMPERATURE, 15.5, "BMP180"));
    System.out.println("Generated XDR:" + xdr);
    
    String mda = generateMDA("II", 1013.25, 25, 12, 75, 50, 9, 270, 255, 12);
    System.out.println("Generated MDA:" + mda);
  }
}
