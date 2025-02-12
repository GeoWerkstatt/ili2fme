/* This file is part of the ili2fme project.
 * For more information, please see <http://www.eisenhutinformatik.ch/interlis/ili2fme/>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package ch.ehi.fme;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import COM.safe.fme.pluginbuilder.IFMEMappingFile;
import COM.safe.fme.pluginbuilder.IFMEReader;
import COM.safe.fme.pluginbuilder.IFMEReaderCreator;
import COM.safe.fme.pluginbuilder.IFMEWriter;
import COM.safe.fme.pluginbuilder.IFMEWriterCreator;
import COM.safe.fmeobjects.IFMECoordSysManager;
import COM.safe.fmeobjects.IFMELogFile;
import COM.safe.fmeobjects.IFMESession;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.settings.Settings;
import ch.ehi.basics.types.OutParam;
import ch.interlis.ili2fme.FmeLogListener;
import ch.interlis.ili2fme.Ili2Reader;
import ch.interlis.ili2fme.Ili2Writer;
import ch.interlis.iox_j.inifile.IniFileReader;
import ch.interlis.iox_j.inifile.MetaConfig;
import ch.interlis.iox_j.validator.ValidationConfig;

/** Factory called by FME to create the reader/writer.
 * @author ce
 * @version $Revision: 1.0 $ $Date: 20.01.2005 $
 */
public class Main implements IFMEReaderCreator, IFMEWriterCreator {
	static public final String ILI2FME_FORMAT_NAME="ch.ehi.fme.Main";
	// ili2fme arguments
	static public final String MODELS="MODELS";
	static public final String MODEL_DIR="MODEL_DIR";
	static public final String CREATE_LINETABLES="CREATE_LINETABLES";
    static public final String ILI1_LINETABLES="ILI1_LINETABLES"; // Polygon%Raw%Polygon+Raw
    static public final String META_CONFIG="META_CONFIG";
    static public final String VALIDATE="VALIDATE";
    static public final String VALIDATE_MULTIPLICITY="VALIDATE_MULTIPLICITY";
    static public final String VALIDATE_CONFIG="VALIDATE_CONFIG";
	static public final String USE_LINETABLES="USE_LINETABLES";
	static public final String SKIP_POLYGONBUILDING="SKIP_POLYGONBUILDING";
	static public final String INHERITANCE_MAPPING="INHERITANCE_MAPPING";
	static public final String CREATEFEATURETYPE4ENUM="CREATEFEATURETYPE4ENUM";
	static public final String TRACEMSGS="TRACEMSGS";
	static public final String ILI1_ADDDEFVAL="ILI1_ADDDEFVAL";
	static public final String ILI1_ENUMASITFCODE="ILI1_ENUMASITFCODE";
    public static final Object ILI1_IGNOREPOLYGONBUILDINGERRORS ="ILI1_IGNOREPOLYGONBUILDINGERRORS";
	static public final String CHECK_UNIQUEOID="CHECK_UNIQUEOID";
	static public final String TRIM_VALUES="TRIM_VALUES";
	static public final String ILI1_RENUMBERTID="ILI1_RENUMBERTID"; 
	static public final String XTFDIR_PLACEHOLDER="%XTF_DIR";
	static public final String DATA_PLACEHOLDER="%DATA";
	static public final String DEPRECATED_XTF_PLACEHOLDER="XTF";
	static public final String FME_COORDINATE_SYSTEM="COORDINATE_SYSTEM";
	static public final String GEOMETRY_ENCODING="GEOMETRY_ENCODING";
	static public final String GEOM_ATTR_MAPPING="GEOM_ATTR_MAPPING";
	static public final String HTTP_PROXYHOST="HTTP_PROXYHOST";
	static public final String HTTP_PROXYPORT="HTTP_PROXYPORT";
	static public final String TOPICS_FILTER="TOPICS_FILTER";
	// format feature types
	static public final String XTF_TRANSFER="XTF_TRANSFER";
	static public final String XTF_BASKETS="XTF_BASKETS";
	static public final String XTF_DELETEOBJECT="XTF_DELETEOBJECT";
	static public final String XTF_ENUMS="XTF_ENUMS";
	static public final String XTF_ERRORS="XTF_ERRORS";
	// format attributes
	static public final String XTF_ID="xtf_id";
	static public final String XTF_BASKET="xtf_basket";
	static public final String XTF_TOPIC="xtf_topic";
	static public final String XTF_STARTSTATE="xtf_startstate";
	static public final String XTF_ENDSTATE="xtf_endstate";
	static public final String XTF_CONSISTENCY="xtf_consistency";
	static public final String XTF_CLASS="xtf_class";
	static public final String XTF_GEOMATTR="xtf_geomattr";
	static public final String XTF_GEOMTYPE="xtf_geomtype";
	static public final String XTF_OPERATION="xtf_operation";
	static public final String ORDERPOS="orderPos";
	static public final String XTF_ENUMTHIS="thisEnum";
	static public final String XTF_ENUMBASE="baseEnum";
	static public final String XTF_ENUMILICODE="iliCode";
	static public final String XTF_ENUMITFCODE="itfCode";
	static public final String XTF_ENUMSEQ="seq";
	static public final String XTF_OIDSPACE="oidspace";
	static public final String XTF_OIDNAME="name";
	static public final String XTF_OIDDOMAIN="oiddomain";
	static public final String XTF_COMMENT="comment";
	static public final String XTF_ERRORS_ILINAME="iliname";
	static public final String XTF_ERRORS_MESSAGE="message";
	static public final String XTF_ERRORS_TID="tid";
	
	// xtf types
	static public final String ID_TYPE="xtf_char(200)";
	static public final String ILINAME_TYPE="xtf_char(255)";
	static public final String STATE_TYPE="xtf_char(250)";
	static public final String CONSISTENCY_TYPE="xtf_char(12)";
	static public final String OPERATION_TYPE="xtf_char(6)";
	static public final String XTF_ENUMITFCODE_TYPE="xtf_int16";
	static public final String XTF_ENUMSEQ_TYPE="xtf_int16";
	//static public final String ILIQNAME_TYPE="xtf_char(255)"; //757 3*255+2 "Model.Topic.Class"
	// XML tag
	static public final String DELETE_TAG="DELETE";
	// name of jar file
	static public final String ILI2FME_JAR="ili2fme.jar";
    public static final String SETTING_REFERENCE_DATA = "ch.ehi.ili2fme.refernceData";
    public static final String SETTING_VALIDATION_CONFIG = "ch.ehi.ili2fme.validationConfig";
	public IFMEReader createReader(IFMEMappingFile mappingFile,
			IFMELogFile logFile,
			IFMECoordSysManager coordSysMan,
			IFMESession session,
			String readerTypeName,
			String readerKeyword)

		throws Exception 
	{
		//logFile.logMessageString("createReader()",IFMELogFile.FME_INFORM);
		logFile.logMessageString("ili2fme-"+getVersion(),IFMELogFile.FME_INFORM);
		logFile.logMessageString("ili2c-"+ch.interlis.ili2c.Main.getVersion(),IFMELogFile.FME_INFORM);
		logFile.logMessageString("iox-ili-"+ch.interlis.iox_j.IoxUtility.getVersion(),IFMELogFile.FME_INFORM);
		logFile.logMessageString("java.version "+System.getProperty("java.version"),IFMELogFile.FME_INFORM);
		logFile.logMessageString("maxMemory "+java.lang.Runtime.getRuntime().maxMemory()/1024L+" KB",IFMELogFile.FME_INFORM);
		Ili2Reader ret=null;
		try{	
			ret=new Ili2Reader(session,mappingFile,readerKeyword,logFile);
		}catch(Exception ex){
			logFile.logMessageString("createReader() "+ex.getMessage(),IFMELogFile.FME_ERROR);
			throw ex;
		}catch(Throwable ex){
			logFile.logMessageString("createReader() "+ex.getMessage(),IFMELogFile.FME_ERROR);
			throw new Exception(ex);
		}
		return ret;
	}

	public IFMEWriter createWriter(
				IFMEMappingFile mappingFile,
				IFMELogFile logFile,
				IFMECoordSysManager coordSysMan,
				IFMESession session,
				String readerTypeName,
				String readerKeyword)
		throws Exception 
	{
        logFile.logMessageString("ili2fme-"+getVersion(),IFMELogFile.FME_INFORM);
        logFile.logMessageString("ili2c-"+ch.interlis.ili2c.Main.getVersion(),IFMELogFile.FME_INFORM);
        logFile.logMessageString("iox-ili-"+ch.interlis.iox_j.IoxUtility.getVersion(),IFMELogFile.FME_INFORM);
        logFile.logMessageString("java.version "+System.getProperty("java.version"),IFMELogFile.FME_INFORM);
        logFile.logMessageString("maxMemory "+java.lang.Runtime.getRuntime().maxMemory()/1024L+" KB",IFMELogFile.FME_INFORM);
		//ch.interlis.ili2fme.DebugUtil.patchPoint(logFile);
		Ili2Writer ret=null;
		try{
			ret=new Ili2Writer(session,mappingFile,readerTypeName,readerKeyword,logFile);
		}catch(Exception ex){
			logFile.logMessageString("createWriter()"+ex.getMessage(),IFMELogFile.FME_ERROR);
			throw ex;
		}catch(Throwable ex){
			logFile.logMessageString("createWriter() "+ex.getMessage(),IFMELogFile.FME_ERROR);
			throw new Exception(ex);
		}
		return ret;
	}
	public static Settings readMetaConfig(File metaConfigFile, OutParam<String> baseConfig) throws IOException, ParseException {
        Settings settings=new Settings();
        ValidationConfig metaConfig = IniFileReader.readFile(metaConfigFile);
        baseConfig.value=metaConfig.getConfigValue(MetaConfig.CONFIGURATION, MetaConfig.CONFIG_BASE_CONFIG);
        String referenceData=metaConfig.getConfigValue(MetaConfig.CONFIGURATION, MetaConfig.CONFIG_REFERENCE_DATA);
        settings.setValue(SETTING_REFERENCE_DATA,referenceData);
        String validConfig=metaConfig.getConfigValue(MetaConfig.CONFIGURATION, MetaConfig.CONFIG_VALIDATOR_CONFIG);
        settings.setValue(SETTING_VALIDATION_CONFIG,validConfig);
        return settings;
    }

    public static java.util.Map<String,String> getPathMap(String xtffile,String appHome)
    {
        java.util.HashMap<String,String> pathMap=new java.util.HashMap<String,String>();
        if(xtffile!=null){
            pathMap.put(XTFDIR_PLACEHOLDER,new java.io.File(xtffile).getAbsoluteFile().getParent());
        }else{
            pathMap.put(XTFDIR_PLACEHOLDER,null);
        }
        //pathMap.put(Ili2cSettings.JAR_DIR,appHome);
        return pathMap;
    }
    private static String version=null;

    public static String getVersion() {
        if (version == null) {
            java.util.ResourceBundle resVersion = java.util.ResourceBundle.getBundle("ch/interlis/ili2fme/Version");
            StringBuffer ret = new StringBuffer(20);
            ret.append(resVersion.getString("version"));
            ret.append('-');
            ret.append(resVersion.getString("versionCommit"));
            version = ret.toString();
        }
        return version;
    }

	private static final boolean doFMELog=true;
	private static final boolean doFileLog=false;
	private static java.util.HashMap fmeListeners=new java.util.HashMap();
	private static ch.ehi.basics.logging.FileListener fileListener=null;
	private static int logFileCount=0;
	public static FmeLogListener setupLogging(IFMELogFile logFile)
	{
		FmeLogListener fmeListener=null;
		if(doFMELog){
			fmeListener=(FmeLogListener)fmeListeners.get(logFile);
			if(fmeListener==null){
				fmeListener=new FmeLogListener(logFile);
				fmeListeners.put(logFile,fmeListener);
				EhiLogger.getInstance().addListener(fmeListener);
			}
			fmeListener.incrCount();
		}
		if(logFileCount==0){
			if(doFileLog){
				fileListener=new ch.ehi.basics.logging.FileListener(new java.io.File("c:/tmp/ili2fme.log"));
				EhiLogger.getInstance().addListener(fileListener);
			}
			EhiLogger.getInstance().removeListener(ch.ehi.basics.logging.StdListener.getInstance());
			//EhiLogger.getInstance().setTraceFilter(false);
		}
		logFileCount++;
		return fmeListener;
		
	}
	public static void endLogging(FmeLogListener fmeListener){
		logFileCount--;
		if(logFileCount==0){
			EhiLogger.getInstance().addListener(ch.ehi.basics.logging.StdListener.getInstance());
		}
		if(fmeListener!=null){
			if(doFMELog){
				fmeListener.decrCount();
				if(fmeListener.getCount()==0){
					EhiLogger.getInstance().removeListener(fmeListener);
				}
			}
			fmeListener=null;
		}
		if(logFileCount==0){
			if(fileListener!=null){
				if(doFileLog){
					EhiLogger.getInstance().removeListener(fileListener);
					fileListener.close();
					fileListener=null;
				}
			}
		}
	}
}
