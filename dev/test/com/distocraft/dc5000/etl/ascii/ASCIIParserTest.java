/**
 * -----------------------------------------------------------------------
 *     Copyright (C) 2011 LM Ericsson Limited.  All rights reserved.
 * -----------------------------------------------------------------------
 */
package com.distocraft.dc5000.etl.ascii;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.ericsson.eniq.common.testutilities.DirectoryHelper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ssc.rockfactory.RockFactory;

import com.distocraft.dc5000.common.StaticProperties;
import com.distocraft.dc5000.etl.parser.MeasurementFileImpl;
import com.distocraft.dc5000.etl.parser.ParseSession;
import com.distocraft.dc5000.etl.parser.ParserDebugger;
import com.distocraft.dc5000.etl.parser.SourceFile;
import com.distocraft.dc5000.etl.parser.TransformerCache;
import com.distocraft.dc5000.repository.cache.DFormat;
import com.distocraft.dc5000.repository.cache.DItem;
import com.distocraft.dc5000.repository.cache.DataFormatCache;

/**
 * @author esuramo
 * @since 2011
 *
 */
public class ASCIIParserTest {

  private static Field mainParserObject;

  private static Field techPack;

  private static Field setType;

  private static Field setName;

  private static Field status;

  private static Field workerName;
  
  private transient Field block;
  
  private transient Field rowDelimLength;
 
  private transient Method readLine;
    
  private transient Method readHeader;
  
  private transient Field log;
  
  private transient Field br;
  
  private transient Field props;

  private SourceFile sourceFile;
  
  private static Constructor<StaticProperties> staticProp; 

  private static Constructor<SourceFile> sourceFileC;

  private static Constructor<MeasurementFileImpl> measurementFileImplC;

  private static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"), ASCIIParserTest.class.getSimpleName());


  private class StubbedASCIIParser extends ASCIIParser {

    // private BufferedReader br;
    @Override
    protected void setData(final SourceFile sf) throws Exception {

      InputStreamReader isr = null;
      isr = new InputStreamReader(sf.getFileInputStream());
      br = new BufferedReader(isr);

    }
  }

  @BeforeClass
  public static void init() {
    DirectoryHelper.mkdirs(TMP_DIR);
    try {
      techPack = ASCIIParser.class.getDeclaredField("techPack");

      setType = ASCIIParser.class.getDeclaredField("setType");
      setName = ASCIIParser.class.getDeclaredField("setName");
      status = ASCIIParser.class.getDeclaredField("status");
      workerName = ASCIIParser.class.getDeclaredField("workerName");
      mainParserObject = ASCIIParser.class.getDeclaredField("mainParserObject");
           

      techPack.setAccessible(true);
      setType.setAccessible(true);
      setName.setAccessible(true);
      status.setAccessible(true);
      workerName.setAccessible(true);
      mainParserObject.setAccessible(true);

      sourceFileC = SourceFile.class.getDeclaredConstructor(new Class[] { File.class, Properties.class,
          RockFactory.class, RockFactory.class, ParseSession.class, ParserDebugger.class, Logger.class });
      measurementFileImplC = MeasurementFileImpl.class.getDeclaredConstructor(new Class[] { SourceFile.class,
          String.class, String.class, String.class, String.class, Logger.class });

      StaticProperties.class.getDeclaredConstructor(new Class[] {});

      sourceFileC.setAccessible(true);
      measurementFileImplC.setAccessible(true);

    } catch (Exception E) {
      E.printStackTrace();
    }

  }

  @AfterClass
  public static void afterClass(){
    DirectoryHelper.delete(TMP_DIR);
  }

  @Test
  public void testInit() throws SecurityException, NoSuchFieldException {

    final ASCIIParser ap = new ASCIIParser();
    ap.init(null, "tp", "st", "sn", "wn");

    try {
      final String expected = "null,tp,st,sn,wn,1";
      final String actual = (String) mainParserObject.get(ap) + "," + techPack.get(ap) + "," + setType.get(ap) + ","
          + setName.get(ap) + "," + workerName.get(ap) + "," + status.get(ap);

      assertEquals("testInit()",expected, actual);

    } catch (Exception e) {
      fail("testInit() failed");
    }
  }

  @Test
  public void parseTest() throws Exception {

    final File out = new File(TMP_DIR, "out1"); // folder where output folder and
    final File techPackDir = new File(out, "techPack");
    final File outputfile = new File(techPackDir, "fname2_worker"); //The file where parsed information will be written.
    final File sampleTopologyFile = new File(TMP_DIR, "DIM_E_SGEH_SGSN.Topology.23052011"); // Input File to the parser.

    try {

      out.mkdir();
      /* Initializing transformer cache */
      new TransformerCache();

      /* Creating new instance of DataFormatCache */
      final Constructor<DataFormatCache> dfcConstructor = DataFormatCache.class.getDeclaredConstructor(new Class[] {});
      dfcConstructor.setAccessible(true);
      final DataFormatCache dfcInstance = dfcConstructor.newInstance(new Object[] {});

      /* Initializing lists of interface names and add it to dfc instance */
      final HashSet<String> IFaceNames = new HashSet<String>();
      final HashMap<String, DFormat> tagMap = new HashMap<String, DFormat>();
      final DFormat dfObj2 = new DFormat("ifname", "tid", "dfid", "fname2", "trID2");

      final List<DItem> dataList2 = new ArrayList<DItem>();
      dataList2.add(new DItem("filename", 0, "header", null, "varchar", 32, 0));
      dataList2.add(new DItem("header1", 1, "SGSN_NAME", null, "varchar", 32, 0));
      dataList2.add(new DItem("header2", 2, "PASSWORD", null, "varchar", 32, 0));
      dataList2.add(new DItem("header3", 3, "USERNAME", null, "varchar", 32, 0));
      dataList2.add(new DItem("header4", 4, "EVENT_PATH", null, "varchar", 32, 0));
      dataList2.add(new DItem("header5", 5, "POOLNAME", null, "varchar", 32, 0));
      dataList2.add(new DItem("header6", 6, "IP_ADDRESS", null, "varchar", 32, 0));
      dataList2.add(new DItem("header7", 7, "NE_VERSION", null, "varchar", 32, 0));
      dataList2.add(new DItem("header8", 8, "VENDOR", null, "varchar", 32, 0));
      dataList2.add(new DItem("header9", 9, "STATUS", null, "varchar", 32, 0));
      dataList2.add(new DItem("header10", 10, "CREATED", null, "varchar", 32, 0));
      dataList2.add(new DItem("header10", 11, "MODIFIED", null, "varchar", 32, 0));
      dataList2.add(new DItem("header10", 12, "MODIFIER", null, "varchar", 32, 0));

      dfObj2.setItems(dataList2);

      tagMap.put("if2_DIM_E_SGEH_SGSN.Topology.23052011", dfObj2);    // tagID = DIM_E_SGEH_SGSN.Topology.23052011

//      final Field if_names = dfcInstance.getClass().getDeclaredField("if_names");
//      final Field it_map = dfcInstance.getClass().getDeclaredField("it_map");
//      if_names.setAccessible(true);
//      it_map.setAccessible(true);
//      if_names.set(dfcInstance, IFaceNames);
//      it_map.set(dfcInstance, tagMap);

      /* Reflecting the dfc field and initialize it with our own instance */
      final Field dfc = DataFormatCache.class.getDeclaredField("dfc");
      dfc.setAccessible(true);
      dfc.set(DataFormatCache.class, dfcInstance);

      final Properties prop = new Properties();
      prop.setProperty("baseDir", TMP_DIR.getPath()); // where out folder is created
      prop.setProperty("interfaceName", "if2");
      prop.setProperty("dc5000.config.directory", TMP_DIR.getPath());
      prop.setProperty("column_delimiter", "\\|");
      prop.setProperty("row_delimiter", "\n");
      prop.setProperty("tag_id_mode", "1");
      prop.setProperty("data_id_mode", "0");
      prop.setProperty("datatime_mode", "0");
      prop.setProperty("buffer_size", "10000");
      prop.setProperty("row_delimiter_size", "1");
      prop.setProperty("header_skip", "1");
      prop.setProperty("header_in_row", "0");
      prop.setProperty("outDir", out.toString());

      /* Test mode on */
      MeasurementFileImpl.setTestMode(true);

      //Creating input file with two lines and two new line characters
      final FileWriter fwrite = new FileWriter(sampleTopologyFile);
      fwrite.write("SGSN_NAME|IP_ADDRESS|USERNAME|PASSWORD|NE_VERSION|EVENT_PATH|POOLNAME\n");
      fwrite
          .write("DUMMY_MME3|0A0A0A01000000000000000000000000|sysadm|letmein123|2011|/tmp/OMS_LOGS/ebs/ready|DUMMY_POOL\n");
      fwrite
          .write("DUMMY_MME4|0A0A0A01000000000000000000000000|sysadm|sysadm|2011|/tmp/OMS_LOGS/ebs/ready|DUMMY_POOL\n");
      fwrite.flush();
      fwrite.close();

      final SourceFile sfile = sourceFileC
          .newInstance(new Object[] { sampleTopologyFile, prop, null, null, null, null, null });

      StubbedASCIIParser ap = new StubbedASCIIParser();
      ap.init(null, "techPack", null, null, "worker");
      ap.parse(sfile, "techPack", "setType", "setName");

      assertTrue("Required file "+outputfile.getPath()+" not created by parser!", outputfile.exists());
      BufferedReader br = new BufferedReader(new FileReader(outputfile));
      String line1 = br.readLine();
      String line2 = br.readLine();
      String expected1 = "DUMMY_MME3\tletmein123\tsysadm\t/tmp/OMS_LOGS/ebs/ready\tDUMMY_POOL\t0A0A0A01000000000000000000000000\t2011";
      String expected2 = "DUMMY_MME4\tsysadm\tsysadm\t/tmp/OMS_LOGS/ebs/ready\tDUMMY_POOL\t0A0A0A01000000000000000000000000\t2011";
      br.close();

      assertEquals("Testing parse() 1st line",expected1, line1.trim());
      assertEquals("Testing parse() 2nd line",expected2, line2.trim());
      
      setSourceFile(sfile);
      setDataTest();

    } finally {
      sampleTopologyFile.delete();
      outputfile.delete();
      techPackDir.delete();
      out.delete();
    }

  }


  private void setSourceFile(final SourceFile sfile) throws Exception {
    
    this.sourceFile = sfile;
  }
  
  public void setDataTest() throws Exception{

    
    ASCIIParser asciiParser = new ASCIIParser();
    asciiParser.init(null, "techPack", null, null, "worker");
   
    
 Properties prop = (Properties) props.get(StaticProperties.class);
 
    if(prop==null){      
      prop = new Properties();
      props.set(StaticProperties.class, prop);
    }
 
    BufferedReader testReader = getBr(asciiParser);
   
    String line =  "";
    
    if((line = testReader.readLine())!=null){
      assertEquals("Testing setData method charsetName is null",line,"SGSN_NAME|IP_ADDRESS|USERNAME|PASSWORD|NE_VERSION|EVENT_PATH|POOLNAME");
    }
    
    String charSet  = "";
    if(prop.getProperty("charsetName")!=null){
      
      charSet  = prop.getProperty("charsetName");
    }
      
      
    
      prop.setProperty("charsetName","UTF-8");
    
      
      props.set(StaticProperties.class, prop);
      
      testReader = getBr(asciiParser);
      
      if((line = testReader.readLine())!=null){
        assertEquals("Testing setData method charsetName is null",line,"SGSN_NAME|IP_ADDRESS|USERNAME|PASSWORD|NE_VERSION|EVENT_PATH|POOLNAME");
      }
      
      if(!charSet.equals("")){        
        prop.setProperty("charsetName",charSet);
      }else{
        prop.remove("charsetName");
      }
      
      prop.setProperty("charsetName",charSet);
      
    
 }
   
  
  
  public BufferedReader getBr(final ASCIIParser asciiParser) throws Exception{
    log.set(asciiParser, Logger.getLogger("etl.Test"));
    asciiParser.setData(sourceFile);   
 
    return (BufferedReader) br.get(asciiParser);
  }
  
  
 
    
  

  @Before
  public void setUp() throws SecurityException, NoSuchFieldException, NoSuchMethodException {


    block = ASCIIParser.class.getDeclaredField("block");
    
    rowDelimLength = ASCIIParser.class.getDeclaredField("rowDelimLength");

    readLine = ASCIIParser.class.getDeclaredMethod("readLine", String.class);   
    
    readHeader = ASCIIParser.class.getDeclaredMethod("readHeader",String.class,String.class); 
       
    log = ASCIIParser.class.getDeclaredField("log"); 
    
    br = ASCIIParser.class.getDeclaredField("br"); 
    
    staticProp = StaticProperties.class.getDeclaredConstructor();
    
    props = StaticProperties.class.getDeclaredField("props");
    
    readLine.setAccessible(true);
    readHeader.setAccessible(true);
    log.setAccessible(true);
    br.setAccessible(true);
    rowDelimLength.setAccessible(true);
    block.setAccessible(true);
    staticProp.setAccessible(true);
    props.setAccessible(true);
 
  }

  
  @Test
  public void readLineTest() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

    ASCIIParser asciiParser = new ASCIIParser();
    
    block.set(asciiParser,null);    
    
    assertEquals("Checking for block =null",readLine.invoke(asciiParser, "\t"),null);
    
    asciiParser = new ASCIIParser();
   
    block.set(asciiParser,"DUMMY_MME3\t0A0A0A01000000000000000000000000\tsysadm\tletmein123\t2011\t/tmp/OMS_LOGS/ebs/ready\tDUMMY_POOL"); 
        
    readLine.setAccessible(true);
    
    log.set(asciiParser, Logger.getLogger("etl.Test"));
      
    assertEquals("Checking for readLine method Functionality",readLine.invoke(asciiParser, "\t"),"DUMMY_MME3");
    
    rowDelimLength.set(asciiParser, 1);
    
    assertEquals("Checking for the blcok value if(result.length > 1)",block.get(asciiParser),"0A0A0A01000000000000000000000000\tsysadm\tletmein123\t2011\t/tmp/OMS_LOGS/ebs/ready\tDUMMY_POOL");
    
    /*
     * Test case to test for the result length is not greater than 1
     */
   String homeDir = System.getProperty("user.home");
   File sampleTopologyFile = new File(homeDir, "DIM_E_SGEH_SGSN.Topology.23052011"); // Input File to the parser.
 

    try{
      
      createTopologyFile(asciiParser,sampleTopologyFile,"DUMMY_MME3\t0A0A0A01000000000000000000000000\tsysadm\tletmein123\t2011\t/tmp/OMS_LOGS/ebs/ready\tDUMMY_POOL");
      
      block.set(asciiParser, "");
     
      assertEquals("Checking for reading the Next Line",readLine.invoke(asciiParser, "\t") ,"DUMMY_MME3");
     
      
      /*
       * Test case to test when the file reached the EOF character.
       * Create an empty file to parse
       */
      createTopologyFile(asciiParser,sampleTopologyFile,null);
      
      block.set(asciiParser, "test");
      
      // Test for finalBlock !=null
      assertEquals("Test for finalBlock!=null",readLine.invoke(asciiParser, "\t") ,"test");
      
      
      //Test for finalBlock==null,since block is null for this time.
      assertEquals("Checking for final Block==null",readLine.invoke(asciiParser, "\t") ,null);
      
      
    } catch (IOException e) {
      // TODO Auto-generated catch block
      fail("Failed while executing readLineTest()");
    }finally{
      sampleTopologyFile.delete();
    }
    
    
  }

  private void createTopologyFile(final ASCIIParser asciiParser,final File sampleTopologyFile,final String string) throws IOException, IllegalArgumentException, IllegalAccessException {
    
      BufferedWriter bw = new BufferedWriter(new FileWriter(sampleTopologyFile));
      if(string!=null){
        bw.write(string);       
      }
 
      bw.close();
      br.set(asciiParser,new BufferedReader(new FileReader(sampleTopologyFile)));
  }

  @Test
  public void readHeaderTest()  { 
   
      try{
      ASCIIParser ascii = new ASCIIParser();
      log.set(ascii, Logger.getLogger("etl.Test"));

      block.set(ascii,null);    
      
      List<String> list = new ArrayList<String>();
      list.add("XXX");
      list.add("YYY");
      list.add("ZZZ");
      
      
      String headerLine = "XXX\tYYY\tZZZ";
      String delimiter = "\t";
      
      assertEquals("Checking readHeader method",readHeader.invoke(ascii,headerLine,delimiter),list);
      }catch(Exception e){
       fail("Exception in executing readHeaderTest\t"+e.getMessage());
      }}
  
  @Test(expected=NullPointerException.class)
  public void runTest() throws IllegalArgumentException, IllegalAccessException{
    
    ASCIIParser asciiParser = new ASCIIParser();asciiParser.run();
  }

}
