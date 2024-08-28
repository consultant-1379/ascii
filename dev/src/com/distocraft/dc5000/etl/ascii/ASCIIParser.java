package com.distocraft.dc5000.etl.ascii;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.Naming;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.xml.sax.helpers.DefaultHandler;

import com.distocraft.dc5000.common.RmiUrlFactory;
import com.distocraft.dc5000.common.StaticProperties;
import com.distocraft.dc5000.etl.parser.Main;
import com.distocraft.dc5000.etl.parser.MeasurementFile;
import com.distocraft.dc5000.etl.parser.Parser;
import com.distocraft.dc5000.etl.parser.SourceFile;
import com.distocraft.dc5000.etl.parser.utils.FlsUtils;
import com.ericsson.eniq.enminterworking.EnmInterCommonUtils;
import com.ericsson.eniq.enminterworking.IEnmInterworkingRMI;

/**
 * Adapter implementation that reads generic column (and row) determined ASCII measurement data. <br>
 * <br>
 * <table border="1" width="100%" cellpadding="3" cellspacing="0">
 * <tr bgcolor="#CCCCFF" class="TableHeasingColor">
 * <td colspan="4"><font size="+2"><b>Parameter Summary</b></font></td>
 * </tr>
 * <tr>
 * <td><b>Name</b></td>
 * <td><b>Key</b></td>
 * <td><b>Description</b></td>
 * <td><b>Default</b></td>
 * </tr>
 * <tr>
 * <td>Column delimiter</td>
 * <td>column_delimiter</td>
 * <td>Character (String) that separates different columns in sourcefile.</td>
 * <td>Tab character (shash-t)</td>
 * </tr>
 * <tr>
 * <td>Row delimiter</td>
 * <td>row_delimiter</td>
 * <td>Character (String) that separates different rows in sourcefile.</td>
 * <td>New line character (slash-n)</td>
 * </tr>
 * <tr>
 * <td>TagID mode</td>
 * <td>tag_id_mode</td>
 * <td>Defines the discovery method of mesurement identification (TAGID).<br>
 * 0 = TAGID is is predefined in parameter named tag_id<br>
 * 1 = TAGID is parsed from name pf sourcefile using regexp pattern defined in parameter named tag_id.</td>
 * <td>1 (from name of sourcefile)</td>
 * </tr>
 * <tr>
 * <td>TagID / TagID filename pattern</td>
 * <td>tag_id</td>
 * <td>Defines predefined TAGID for measurement type or defines regexp pattern that is used to parse TAGID from the name of sourcefile.</td>
 * <td>&nbsp;
 * <td/>
 * </tr>
 * <tr>
 * <td>Data identification</td>
 * <td>data_id_mode</td>
 * <td>Defines the method of measurement colum identification.<br>
 * 0 = columns are identified by parsing header row of file.<br>
 * 1 = columns are identified by parsing header row that is defined in parameter named header_row.<br>
 * 2 = columns are identfied only by order number (name of first column is "1" and second is "2" etc).</td>
 * <td>2 (identified by order number)</td>
 * </tr>
 * <tr>
 * <td>Header row</td>
 * <td>header_row</td>
 * <td>Defines fixed header row. This parameter is used if data_id_mode is 1. Value of this parameter is parsed exactly like it would have been
 * discovered as first row of sourcefile.</td>
 * <td>&nbsp;
 * <td/>
 * </tr>
 * <tr>
 * <td>Datatime mode</td>
 * <td>datatime_mode</td>
 * <td>Determines how fixed column DATATIME is discovered from this file.<br>
 * 0 = DATATIME column handling is disabled.<br>
 * 1 = Column specified in parameter named datatime_column. Value of this column is copied into column DATATIME.<br>
 * 2 = DATATIME column is defined by parsing name of sourcefile using regexp pattern
 * <td>0 (handling disabled)</td>
 * </tr>
 * <tr>
 * <td>Column name / Filename pattern</td>
 * <td>datatime_column</td>
 * <td>This parameter defines DATATIME column name if datatime_mode = 1.<br>
 * This paramter defines regexp that is used to parse DATATIME from sourcefile name if datatime_mode = 2.</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <td>Header skip</td>
 * <td>header_skip</td>
 * <td>Defines number of rows that are skipped from the beginning of the file in sense of data. If there is a header row(s) in source file this
 * parameter should be set. Header row shall be red (if configured) from the first row of the file regardless of this parameter.</td>
 * <td>0</td>
 * </tr>
 * <tr>
 * <td>Header in row</td>
 * <td>header_in_row</td>
 * <td>If header is read in the file this parameter determines the row header is in the file. Row count starts from 0. If this parameter is not
 * defined header is read from the first line of file.</td>
 * <td>0</td>
 * </tr>
 * <tr>
 * <td>&nbsp;</td>
 * <td>row_delimiter_size</td>
 * <td>Defines how many characters is deleted row delimiter is removed.</td>
 * <td>2</td>
 * </tr>
 * <tr>
 * <td>&nbsp;</td>
 * <td>buffer_size</td>
 * <td>Defines the size of the inner buffer size, how many characters are read from datafile at one time.</td>
 * <td>10000</td>
 * </tr>
 * </table>
 * </table> <br>
 * <br>
 * <table border="1" width="100%" cellpadding="3" cellspacing="0">
 * <tr bgcolor="#CCCCFF" class="TableHeasingColor">
 * <td colspan="2"><font size="+2"><b>Added DataColumns</b></font></td>
 * </tr>
 * <tr>
 * <td><b>Column name</b></td>
 * <td><b>Description</b></td>
 * </tr>
 * <tr>
 * <td>filename</td>
 * <td>contains the filename of the inputdatafile.</td>
 * <tr>
 * <td>DC_SUSPECTFLAG</td>
 * <td>EMPTY</td>
 * </tr>
 * <tr>
 * <td>DIRNAME</td>
 * <td>Conatins full path to the inputdatafile.</td>
 * </tr>
 * <tr>
 * <td>JVM_TIMEZONE</td>
 * <td>contains the JVM timezone (example. +0200)</td>
 * </tr>
 * <tr>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * </tr>
 * </table>
 * 
 * @author lemminkainen, savinen
 * 
 */
public class ASCIIParser extends DefaultHandler implements Parser {

    private static final String JVM_TIMEZONE = new SimpleDateFormat("Z").format(new Date());

    public static final int DATATIME_SKIPPED = 0;

    public static final int DATATIME_FROM_COLUMN = 1;

    public static final int DATATIME_FROM_FILENAME = 2;

    public static final int TAGID_FROM_CONFIG = 0;

    public static final int TAGID_FROM_FILENAME = 1;

    public static final int DATAID_FROM_HEADER = 0;

    public static final int DATAID_FROM_CONFIG = 1;

    public static final int DATAID_FROM_COLUMNS = 2;

    protected BufferedReader br;

    private List counterList;

    private String tagID;

    private Logger log;

    private String rowDelim;

    private String colDelim;

    private int tagIDMode;

    private int dataIDMode;

    private String headerRow;

    private int datatimeMode;

    private String datatimeColumn;

    private SourceFile sf;

    private String filename;

    private Pattern tagPattern;

    private Pattern timePattern;

    private String block = "";

    private int bufferSize = 10000;

    private int rowDelimLength = 1;
    
    private String UTF8_BOM = "\uFEFF";


    //***************** Worker stuff ****************************

    private String techPack;
    private String setType;
    private String setName;
    private int status = 0;
    private Main mainParserObject = null;
    private String workerName = "";

	// ******************60K

	private String nodefdn;

	private String ne_type = "";

	private boolean isFlsEnabled = false;

	private String serverRole = null;

	private IEnmInterworkingRMI multiEs;
	
	private Map<String, String> ossIdToHostNameMap;
	
	private boolean isVerticalTraverse;
             
    private Set<String> writeForEachColumns;
    
    private String eNodeBName = "";
    
    private Map<String, List<String>> writeForEachMap;
    
    private Map<String, String> verticalTraverseData;

	
	/**
	   * Parameters for throughput measurement.
	   */
	  private long parseStartTime;
	  private long totalParseTime;
	  private long fileSize;
	  private int fileCount;
	
	
    @Override
    public void init(final Main main, final String techPack, final String setType, final String setName, final String workerName) {
        this.mainParserObject = main;
        this.techPack = techPack;
        this.setType = setType;
        this.setName = setName;
        this.status = 1;
        this.workerName = workerName;

        String logWorkerName = "";
        if (workerName.length() > 0) {
            logWorkerName = "." + workerName;
        }

        log = Logger.getLogger("etl." + techPack + "." + setType + "." + setName + ".parser.ASCII" + logWorkerName);
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public void run() {

    	try {

    		this.status = 2;
    		SourceFile sf = null;
    		parseStartTime = System.currentTimeMillis();
    		while ((sf = mainParserObject.nextSourceFile()) != null) {

    			try {
    				fileCount++;
    				fileSize += sf.fileSize();                	
    				mainParserObject.preParse(sf);
    				parse(sf, techPack, setType, setName);
    				mainParserObject.postParse(sf);
    			} catch (final Exception e) {
    				mainParserObject.errorParse(e, sf);
    			} finally {
    				mainParserObject.finallyParse(sf);
    			}
    		}
    		totalParseTime = System.currentTimeMillis() - parseStartTime;
    		if (totalParseTime != 0) {
    			log.info("Parsing Performance :: " + fileCount + " files parsed in " + totalParseTime
    					+ " milliseconds, filesize is " + fileSize + " bytes and throughput : " + (fileSize / totalParseTime)
    					+ " bytes/ms.");
    		}
    	} catch (final Exception e) {
    		// Exception catched at top level. No good.
    		log.log(Level.WARNING, "Worker parser failed to exception", e);
    	} finally {
    		this.status = 3;
    	}
    }

    //***************** Worker stuff ****************************

    /**
     * Parse one SourceFile
     * 
     * @see com.distocraft.dc5000.etl.parser.Parser#parse(com.distocraft.dc5000.etl.parser.SourceFile, java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    @Override
    public void parse(final SourceFile sf, final String techPack, final String setType, final String setName) throws Exception {

        this.sf = sf;

        this.filename = sf.getName();
        MeasurementFile mFile = null;
        block = "";

        try {

            colDelim = sf.getProperty("column_delimiter", "\t");
            if (colDelim.length() == 0) {
                colDelim = "\t";
            }
            log.finest("col_delim: " + colDelim);

            rowDelim = sf.getProperty("row_delimiter", "\n");
            if (rowDelim.length() == 0) {
                rowDelim = "\n";
            }
            log.finest("row_delim: " + rowDelim);

            tagIDMode = Integer.parseInt(sf.getProperty("tag_id_mode", "" + TAGID_FROM_FILENAME));
            log.finest("tag_id_mode: " + tagIDMode);

            tagID = sf.getProperty("tag_id", "(.+)");
            log.finest("tag_id: " + tagID);

            try {
                if (tagIDMode == TAGID_FROM_FILENAME) {
                    final String patt = tagID;
                    tagPattern = Pattern.compile(patt);
                    final Matcher m = tagPattern.matcher(filename);
                    if (m.find()) {
                        tagID = m.group(1);
                    }
                }
            } catch (final Exception e) {

                log.log(Level.WARNING, "Error while matching pattern " + tagID + " from filename " + filename + " for tag_id", e);

            }

            dataIDMode = Integer.parseInt(sf.getProperty("data_id_mode", "" + DATAID_FROM_COLUMNS));
            log.finest("data_id_mode: " + dataIDMode);

            headerRow = sf.getProperty("header_row", "");
            log.finest("header_row: " + headerRow);

            datatimeMode = Integer.parseInt(sf.getProperty("datatime_mode", "" + DATATIME_SKIPPED));
            log.finest("datatime_mode: " + datatimeMode);

            datatimeColumn = sf.getProperty("datatime_column", "");
            log.finest("datatime_column: " + datatimeColumn);

            bufferSize = Integer.parseInt(sf.getProperty("buffer_size", Integer.toString(bufferSize)));
            log.finest("buffer_size: " + bufferSize);

            rowDelimLength = Integer.parseInt(sf.getProperty("row_delimiter_size", Integer.toString(rowDelim.length())));
            log.finest("row_delimiter_size: " + rowDelimLength);
                       
            String fileNamePattern = sf.getProperty("fileNameFormat", "");
            log.finest("The pattern to check eNodeBName is " +fileNamePattern);
            
            if (fileNamePattern != null && !fileNamePattern.equals("")) {
            	         			
            	String result = transformFileVariables(filename, fileNamePattern);
                if (result != null) {
                	eNodeBName = result;
                	log.log(Level.FINEST, "the file name "+filename+" matches with the described pattern "+fileNamePattern);
                	log.log(Level.FINEST, "eNodeBName extracted from file ="+eNodeBName);
                } else {
                	log.log(Level.INFO, "the file name "+filename+" does not match with the described pattern "+fileNamePattern);
                	return;
                }
            }
                                  
            String writeForEach = sf.getProperty("writeForEach", "");
            log.finest("The writeForEach is " + writeForEach);
            
            if (writeForEach != null && !writeForEach.equals("")) {
            	writeForEachColumns = new HashSet<>();
            	writeForEachColumns.addAll(Arrays.asList(writeForEach.split(",")));
            	writeForEachMap = new HashMap<>();
            }
            
            isVerticalTraverse = "true".equalsIgnoreCase(sf.getProperty("verticalTraversing", "false"));
            log.finest("isVerticalTraverse: " + isVerticalTraverse);
                 
            try {
                if (datatimeMode == DATATIME_FROM_FILENAME) {
                    final String patt = datatimeColumn;
                    timePattern = Pattern.compile(patt);
                    final Matcher m = timePattern.matcher(filename);
                    if (m.find()) {
                        datatimeColumn = m.group(1);
                    }
                }
            } catch (final Exception e) {

                log.log(Level.WARNING, "Error while matching pattern " + datatimeColumn + " from filename " + filename + " for datatime_column", e);

            }
            mFile = Main.createMeasurementFile(sf, tagID, techPack, setType, setName, this.workerName, log);

            if (null == mFile.getDataformat()) {
                throw new Exception("No Dataformat available for tagID: " + tagID);
            }
            

            final int headerSkip = Integer.parseInt(sf.getProperty("header_skip", "1"));
            log.finest("header_skip: " + headerSkip);

            final int headerInRow = Integer.parseInt(sf.getProperty("header_in_row", "0"));
            log.finest("header_in_row: " + headerInRow);

            this.counterList = new ArrayList();

            setData(sf);

            log.fine("Parsing File: " + sf.getName());
            String header = "";

            if (!isVerticalTraverse) {
            	 for (int i = 0; i < headerSkip; i++) {
                     if (i == headerInRow) {
                         header = readLine(rowDelim);
                     } else {
                         readLine(rowDelim);
                     }
                 }

                 if (dataIDMode == DATAID_FROM_HEADER) {
                     counterList = readHeader(header, colDelim);
                 } else if (dataIDMode == DATAID_FROM_CONFIG) {
                     counterList = readHeader(headerRow, colDelim);
                 } else if (dataIDMode == DATAID_FROM_COLUMNS) {
                     counterList = null;
                 }
            } else {
            	counterList = null;
            	verticalTraverseData = new HashMap<>();
            }
           

            readDataLines(mFile);

            mFile.close();

        } catch (final Exception e) {
            e.printStackTrace();
            if (null == mFile.getDataformat()) {
                throw e;
            } else {
                log.log(Level.WARNING, "General Failure", e);
            }

        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (final Exception e) {
                    log.log(Level.WARNING, "Error closing Reader", e);
                }
            }

            if (mFile != null) {
                try {
                    mFile.close();
                } catch (final Exception e) {
                    log.log(Level.WARNING, "Error closing MeasurementFile", e);
                }
            }
        }

    }

    /**
     * 
     * 
     * @param mFile
     */

    private List readHeader(final String headerLine, final String delim) throws Exception {

        final List list = new ArrayList();
        log.log(Level.FINEST, " header: " + headerLine);

        final String[] result = headerLine.split(delim);
        for (final String value : result) {
            list.add(value);
            log.log(Level.FINEST, " Value: " + value);

        }

        return list;

    }

    private void writeData(final String data, final int index, final MeasurementFile mFile) throws Exception {
        try {
            String key = "";
            if (counterList == null) {
                key = Integer.toString(index).trim();
            } else {
                key = ((String) counterList.get(index)).trim();
            }
            prepareFlsData(key, data);
            mFile.addData(key, data);
            log.log(Level.FINEST, " data element: " + key + " = " + data + " addded to measurement file");

        } catch (final Exception e) {
            log.log(Level.WARNING, " Error while inserting data pair key:" + Integer.toString(index).trim() + " value:" + data, e);
            throw new Exception(" Error while inserting data pair key:" + Integer.toString(index).trim() + " value:" + data, e);
        }
    }
    
    private void prepareFlsData(String key, String data) {
       	if(key.equals("srcNodeFDN") || key.equals("nodeFDN")) {
        	nodefdn=data;
        }
        if(key.equals("nodeType") || key.equals("srcNodeType") || (key.equals("sourceType") && data.length() > 0)) {
        	ne_type=data;
        }
    }
    
    private void handleVerticalTraverse(String[] result) {
    	String key = null;
    	String value = null;
        List<String> valueList = null;
        if (isVerticalTraverse) {
        	key = result[0];
        	if (result.length > 1) {
        		value = result [1];
        	} else {
        		return;
        	}
        	log.log(Level.FINEST, "handleVerticalTraverse: key :"+key+" and value :"+value);
           	if (writeForEachColumns != null && writeForEachColumns.contains(key)) {
        		if (value != null && value.contains(eNodeBName)) {
        			key = "enodeBFDN";
        			verticalTraverseData.put(key, value);
        			log.log(Level.FINEST, "handleVerticalTraverse:Adding enodeBFDN :"+key+" and value :"+value);
        		} else {
        			valueList = writeForEachMap.get(key);
        			if (valueList != null){
        				valueList.add(value);
        			} else {
        				valueList = new ArrayList<>();
        				valueList.add(value);
        			}
        			log.log(Level.FINEST, "handleVerticalTraverse: writeForEachMap: Adding key :"+key+" and valueList :"+valueList);
        			writeForEachMap.put(key, valueList);
        		}
        	} else {
        		prepareFlsData(key, value);
        		verticalTraverseData.put(key, value);
        	}
        }
    }
    
    private void sendDataToFls() {
    	try {
			String dir = sf.getProperty("inDir");
			log.log(Level.FINE, "THE INDIR IS >>>>>>>>>>>>>"+dir);
//			String pattern = ".+/(.+)/.+/.+/.+";
//			String enmDir = transformFileVariables(dir, pattern);
			String enmDir=getOSSIdFromInDir(dir);
			log.log(Level.FINE, "THE ENMDIR IS >>>>>>>>>>>>>>>>>>>"+enmDir);
			String enmAlias = Main.resolveDirVariable(enmDir);
			isFlsEnabled = FlsUtils.isFlsEnabled(enmAlias);
			if (isFlsEnabled) {
				multiEs = (IEnmInterworkingRMI) Naming
						.lookup(RmiUrlFactory.getInstance().getMultiESRmiUrl(EnmInterCommonUtils.getEngineIP()));
				String enmHostName = FlsUtils.getEnmShortHostName(enmAlias);
				try {
					if ( ( nodefdn != null && !nodefdn.equals("") ) && ( ne_type != null && !ne_type.equals("") ) && enmHostName != null){
						log.info("In FLS mode. Adding FDN to the Automatic Node Assignment Blocking Queue "
								+ ne_type + " " + nodefdn + " " + enmHostName);
						multiEs.addingToBlockingQueue(ne_type, nodefdn, enmHostName);
					}
				} catch (Exception abqE) {
					log.log(Level.WARNING, "Exception occured while adding FDN to Blocking Queue!", abqE);
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Exception occured while checking for FLS", e);
		}
    }
    
    private void addDefaultData(MeasurementFile mFile) {
    	// if datetime is parsed from filename...
		mFile.addData("filename", sf.getName());
		mFile.addData("DC_SUSPECTFLAG", "");
		mFile.addData("DIRNAME", sf.getDir());
		mFile.addData("JVM_TIMEZONE", JVM_TIMEZONE);
    }

    /**
     * 
     * 
     * @param mFile
     */
    private void readDataLines(final MeasurementFile mFile) throws Exception {

    	String line;
    	String value;
    	long lineNum = 0;

    	// read line from file
    	line = readLine(rowDelim);
    	log.log(Level.INFO, "dataline: " + line);
	
    	if (null != line) {

    		do {
    			final String[] result = line.split(colDelim);
    			if (isVerticalTraverse) {
    				handleVerticalTraverse(result);
    			} else {
    				try {

    					//if empty row then don't write it out
    					if (0 < line.trim().length()) {
    						for (int i = 0; i < result.length; i++) {
    							value = result[i];
    							log.log(Level.FINEST, "Value: " + value);

    							// if datetime is retrieved from column and column name is correct,
    							// get DATETIME_ID

    							if (datatimeMode == DATATIME_FROM_COLUMN && ((String) counterList.get(i)).equalsIgnoreCase(datatimeColumn)) {
    								mFile.addData("DATETIME_ID", value.trim());
    							}
    							if (datatimeMode == DATATIME_FROM_FILENAME) {
    								mFile.addData("DATETIME_ID", datatimeColumn);
    							}

    							if (mFile.isOpen()) {
    								writeData(value.trim(), i, mFile);
    							}
    						}
    						addDefaultData(mFile);
    						sendDataToFls();
    						mFile.saveData();
    					}
    				} catch (final Exception e) {

    					log.log(Level.WARNING, "Error while parsing dataline, skipping(" + lineNum + "): " + line, e);
    				}
    			}
    			line = readLine(rowDelim);
    			lineNum++;
    		} while (null != line);
    		if (isVerticalTraverse){
    			writeForVTraverse(mFile);
    		}
    	}
    }
    
    private void writeForVTraverse(MeasurementFile mFile) {
    	try {
    		 if(writeForEachMap.get("FDN")== null) {
    			 List<String> arr= new ArrayList<String>();
    			 String val=verticalTraverseData.get("enodeBFDN");
    			 arr.add(val);
    			 writeForEachMap.put("FDN", arr);
    		 }
	      	for (Map.Entry<String, List<String>> entry : writeForEachMap.entrySet()) {
	    		for(String value: entry.getValue()) {
	    			mFile.addData(entry.getKey(), value);
	    			mFile.addData(verticalTraverseData);
	    			addDefaultData(mFile);
	    			mFile.saveData();
	    			log.log(Level.FINEST, "writeForVTraverse:Adding key :"+entry.getKey()+" and value :"+value);
	    		}
	    	}
	    	sendDataToFls();
		} catch (Exception e) {
			log.log(Level.WARNING, "Error while saving data got by vertical traversing",e);
		} finally {
			verticalTraverseData.clear();
		}
    }

    /**
     * Creates new Bufferreader from a file.
     * 
     * @param Filename
     * 
     */
    protected void setData(final SourceFile sf) throws Exception {

        final String charsetName = StaticProperties.getProperty("charsetName", null);
        InputStreamReader isr = null;
        if (charsetName == null) {

            isr = new InputStreamReader(sf.getFileInputStream(), "UTF-8");
            

        } else {

            log.log(Level.FINEST, "InputStreamReader charsetName: " + charsetName);
            isr = new InputStreamReader(sf.getFileInputStream(), charsetName);

        }

        log.log(Level.FINEST, "InputStreamReader Encoding: " + isr.getEncoding());
        br = new BufferedReader(isr);

    }

    /**
     * read characters from reader until eof or delimiter is encountered.
     * 
     * @param Filename
     * 
     */
    private String readLine(final String delimiter) throws Exception {

        //if end of line return with null
        if (null == this.block) {
            return null;
        }

        final char[] tmp = new char[bufferSize];

        while (true) {

            // log.log(Level.FINEST, "buffer: " + block);

            // delimiter found
            final String[] result = this.block.split(delimiter);
            
            if (result.length > 1) {
            	
            	if(result[0].startsWith(UTF8_BOM))
                {
            		log.info("The file starts with BOM Character");
                    result[0]= result[0].substring(1);
                    log.info("The result[0] is" + result[0]);
                	
                }
                // remove discovered token + deliminator from block
                block = block.substring(result[0].length() + rowDelimLength);

                log.log(Level.FINEST, "result: " + result[0]);

                // return found block
                return result[0];

            } else {

                // delimiter not found, read next block
                final int count = br.read(tmp, 0, bufferSize);

                // if end of file return whole block and set block to null value
                if (count == -1) {
                    final String finalBlock = this.block;
                    this.block = null;
                    return finalBlock;
                }

                this.block += new String(tmp);
            }
        }

    }
    
    /**
	 * Lookups variables from filename
	 * 
	 * @param name
	 *            of the input file
	 * @param pattern
	 *            the pattern to lookup from the filename
	 * @return result returns the group(1) of result, or null
	 */
	private String transformFileVariables(String filename, String pattern) {
		String result = null;
		try {
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(filename);
			if (m.matches()) {
				result = m.group(1);
			}
		} catch (PatternSyntaxException e) {
			log.log(Level.SEVERE, "Error performing transformFileVariables for ASCIIParser.", e);
		}
		return result;
	}
	
	/**
	 * Lookups variables from filename
	 * 
	 * @param name
	 *            of the input file
	 * @return result returns OSSId, or null
	 */
	private String getOSSIdFromInDir(String filename) {
		String result = null;
		try {
			if(filename.contains("/"))
			{
				result=filename.split("/")[1];
			}
			else
			{
				log.warning("InDir of the source file is not proper");
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error performing getOSSIdFromInDir ", e);
		}
		return result;
	}
	
    /**
     * Main - testing only
     */
    public static void main(final String args[]) {

    }

}
