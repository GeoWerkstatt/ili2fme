package ch.interlis.ili2fme;

import COM.safe.fmeobjects.FMEException;
import COM.safe.fmeobjects.IFMEArea;
import COM.safe.fmeobjects.IFMECurve;
import COM.safe.fmeobjects.IFMEFactoryPipeline;
import COM.safe.fmeobjects.IFMEFeature;
import COM.safe.fmeobjects.IFMEMultiArea;
import COM.safe.fmeobjects.IFMEMultiCurve;
import COM.safe.fmeobjects.IFMEMultiPoint;
import COM.safe.fmeobjects.IFMENull;
import COM.safe.fmeobjects.IFMEPath;
import COM.safe.fmeobjects.IFMEGeometry;
import COM.safe.fmeobjects.IFMEDonut;
import COM.safe.fmeobjects.IFMEPoint;
import COM.safe.fmeobjects.IFMESession;
import COM.safe.fmeobjects.IFMEGeometryTools;
import COM.safe.fmeobjects.IFMEText;
import ch.interlis.iom.IomObject;
import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.fme.Main;


public class GeometryConverter {
	private int mode=0;
	private IFMESession session=null;
	private IFMEFeature f=null;
	private IFMEFactoryPipeline pipe=null;
	public GeometryConverter(IFMESession session,int converterMode){
		this.session=session;
		f=session.createFeature();
		mode=converterMode;
	}
	private final String DUMMY_TYPE="Dummy";
	private final String DUMMY_ATTR="attr";
	private void initPipe() 
	throws FMEException 
	{
		if(pipe==null){
			pipe=session.createFactoryPipeline("GeometryConverter", null);
			String func=null;
			if(mode==GeometryEncoding.FME_HEXBIN){
				func="@Geometry(FROM_ATTRIBUTE_BINARY_HEX,"+DUMMY_ATTR+")";
			}else if(mode==GeometryEncoding.FME_BIN){
				func="@Geometry(FROM_ATTRIBUTE_BINARY,"+DUMMY_ATTR+")";
			}else{
				// mode==FME_XML
				func="@Geometry(FROM_ATTRIBUTE,"+DUMMY_ATTR+")";
			}
			String factory="FACTORY_DEF * TeeFactory"
				+" INPUT FEATURE_TYPE "+DUMMY_TYPE
				+" OUTPUT FEATURE_TYPE "+DUMMY_TYPE
				+" "+func;
			pipe.addFactory(factory, " ");
		}
	}
	public void dispose()
	{
		if(f!=null){
			f.dispose();
			f=null;
		}
		if(pipe!=null){
			pipe.dispose();
			pipe=null;
		}
	}
	private void setGeometry(IFMEFeature ret, String attrName, IFMEGeometry fmeLine) 
	throws DataException 
	{
		final String ATTR="attr";
		f.setGeometry(fmeLine);
		String func=null;
		if(mode==GeometryEncoding.FME_HEXBIN){
			func="@Geometry(TO_ATTRIBUTE_BINARY_HEX,"+ATTR+")";
		}else if(mode==GeometryEncoding.FME_BIN){
			func="@Geometry(TO_ATTRIBUTE_BINARY,"+ATTR+")";
		}else{
			// mode==FME_XML
			func="@Geometry(TO_ATTRIBUTE,"+ATTR+")";
		}
		 try {
			f.performFunction(func);
		} catch (FMEException ex) {
			throw new DataException(ex);
		}
		try {
			if(mode==GeometryEncoding.FME_BIN){
				 ret.setByteArrayAttribute(attrName, f.getByteArrayAttribute(ATTR));
			}else{
				 ret.setStringAttribute(attrName, f.getStringAttribute(ATTR));
			}
		} catch (FMEException ex) {
			throw new DataException(ex);
		}
	}
	private IFMEGeometry getGeometry(IFMEFeature src, String srcAttr) 
	throws DataException 
	{
		try {
			if(mode==GeometryEncoding.FME_XML){
				String val=src.getStringAttribute(srcAttr);
				IFMEGeometry geom=((IFMEGeometryTools)session.getGeometryTools()).createGeometryFromXML(val);
				return geom;
			}
			initPipe();
			f.setFeatureType(DUMMY_TYPE);
			if(mode==GeometryEncoding.FME_BIN){
				//f.setBinaryAttribute(DUMMY_ATTR, src.getBinaryAttribute(srcAttr));
				f.setByteArrayAttribute(DUMMY_ATTR, src.getByteArrayAttribute(srcAttr));
			}else{
				String val=src.getStringAttribute(srcAttr);
				f.setStringAttribute(DUMMY_ATTR, val);
			}
			pipe.processFeature(f);
			boolean gotOne=pipe.getOutputFeature(f);
			if(!gotOne){
				throw new DataException("failed to get feature back from pipeline");
			}
		} catch (FMEException ex) {
			throw new DataException(ex);
		}
		return f.getGeometry();
	}
	 public void coord2FME(IFMEFeature ret,String attrName,IomObject value)
		throws DataException 
	 {
		 IFMEPoint point=null;
		 try{
			 point=Iox2fme.coord2FME(session,value);
			 setGeometry(ret,attrName,point);
		 }finally{
			 if(point!=null){
				 point.dispose();
				 point=null;
			 }
		 }
	 }

 	public void multicoord2FME(IFMEFeature ret, String attrName, IomObject value)
	throws DataException
	{
		IFMEMultiPoint multiPoint=null;
		try {
			multiPoint = Iox2fme.multicoord2FME(session, value);
			setGeometry(ret, attrName, multiPoint);
		} finally {
			if(multiPoint!=null){
				multiPoint.dispose();
			}
		}
	}

	 public void polyline2FME(IFMEFeature ret,String attrName,IomObject value)
		throws DataException 
	 {
		 IFMEPath fmeLine=null;
		 try{
			 fmeLine=Iox2fme.polyline2FME(session,value,false);
			 setGeometry(ret, attrName, fmeLine);
		 }finally{
			 if(fmeLine!=null){
				 fmeLine.dispose();
				 fmeLine=null;
			 }
		 }
	 }
	public void multipolyline2FME(IFMEFeature ret, String attrName, IomObject value)
	throws DataException
	{
		IFMEMultiCurve multiCurve=null;
		try {
			 multiCurve = Iox2fme.multipolyline2FME(session, value);
			setGeometry(ret, attrName, multiCurve);
		} finally {
			if(multiCurve!=null){
				multiCurve.dispose();
			}
		}
	}

	 public void surface2FME(IFMEFeature ret,String attrName,IomObject value)
		throws DataException 
	 {
		 IFMEArea fmeSurface=null;
		 try{
			 fmeSurface=Iox2fme.surface2FME(session,value);
			 setGeometry(ret, attrName, fmeSurface);
		 }finally{
			 if(fmeSurface!=null){
				 fmeSurface.dispose();
				 fmeSurface=null;
			 }
		 }
	 }

	public void mutlisurface2FME(IFMEFeature ret,String attrName,IomObject value)
	throws DataException {
		IFMEMultiArea fmeMultiArea = null;
		try {
			fmeMultiArea = Iox2fme.multisurface2FME(session, value);
			setGeometry(ret, attrName, fmeMultiArea);
		} finally {
			if (fmeMultiArea != null) {
				fmeMultiArea.dispose();
			}
		}
	}
	 public void FME2polyline(IomObject target,String targetAttr,IFMEFeature src,String srcAttr)
		throws DataException 
	 {
		 IFMEGeometry fmeGeom=null;
			try{
				fmeGeom=getGeometry(src, srcAttr);
				if(fmeGeom instanceof IFMECurve){
					IomObject polyline=Fme2iox.FME2polyline(session,(IFMECurve)fmeGeom);
					target.addattrobj(targetAttr,polyline);
				}else if(fmeGeom instanceof IFMENull){
					// skip it
				}else{
					throw new DataException("unexpected geometry type "+fmeGeom.getClass().getName());
				}
			}finally{
				if(fmeGeom!=null){
					fmeGeom.dispose();
					fmeGeom=null;
				}
			}
	 }

	public void FME2multipolyline(IomObject target, String targetAttr, IFMEFeature src, String srcAttr)
	throws DataException {
		IFMEGeometry fmeGeom = null;
		try {
			fmeGeom = getGeometry(src, srcAttr);
			if (fmeGeom instanceof IFMEMultiCurve) {
				IomObject multipolyline = Fme2iox.FME2multipolyline(session, (IFMEMultiCurve) fmeGeom);
				target.addattrobj(targetAttr, multipolyline);
			} else if (fmeGeom instanceof IFMENull) {
				// skip it
			} else {
				throw new DataException("unexpected geometry type " + fmeGeom.getClass().getName());
			}
		} finally {
			if (fmeGeom != null) {
				fmeGeom.dispose();
			}
		}
	}

	 public void FME2surface(IomObject target,String targetAttr,IFMEFeature src,String srcAttr)
		throws DataException 
	 {
			IFMEGeometry fmeGeom=null;
			try{
				fmeGeom=getGeometry(src,srcAttr);
				if(fmeGeom instanceof IFMEArea){
					IomObject surface=Fme2iox.FME2surface(session,(IFMEArea)fmeGeom);
					target.addattrobj(targetAttr,surface);
				}else if(fmeGeom instanceof IFMENull){
					// skip it
				}else{
					throw new DataException("unexpected geometry type "+fmeGeom.getClass().getName());
				}
			}finally{
				if(fmeGeom!=null){
					fmeGeom.dispose();
					fmeGeom=null;
				}
			}
	 }

	public void FME2multisurface(IomObject target,String targetAttr,IFMEFeature src,String srcAttr)
	throws DataException {
		IFMEGeometry fmeGeom = null;
		try {
			fmeGeom = getGeometry(src, srcAttr);
			if (fmeGeom instanceof IFMEMultiArea) {
				IomObject surface = Fme2iox.FME2multisurface(session, (IFMEMultiArea) fmeGeom);
				target.addattrobj(targetAttr, surface);
			} else if (fmeGeom instanceof IFMENull) {
				// skip it
			} else {
				throw new DataException("unexpected geometry type " + fmeGeom.getClass().getName());
			}
		} finally {
			if (fmeGeom != null) {
				fmeGeom.dispose();
			}
		}
	}

	 public void FME2coord(IomObject target,String targetAttr,IFMEFeature src,String srcAttr)
		throws DataException 
	 {
			IFMEGeometry fmeGeom=null;
			try{
				fmeGeom=getGeometry(src,srcAttr);
				if(fmeGeom instanceof IFMEPoint){
					IomObject coord=Fme2iox.FME2coord((IFMEPoint)fmeGeom);
					target.addattrobj(targetAttr,coord);
				}else if(fmeGeom instanceof IFMEText){
					IomObject coord=Fme2iox.FME2coord(((IFMEText)fmeGeom).getLocationAsPoint());
					target.addattrobj(targetAttr,coord);
				}else if(fmeGeom instanceof IFMENull){
					// skip it
				}else{
					throw new DataException("unexpected geometry type "+fmeGeom.getClass().getName());
				}
			}finally{
				if(fmeGeom!=null){
					fmeGeom.dispose();
					fmeGeom=null;
				}
			}
	 }

	public void FME2multicoord(IomObject target, String targetAttr, IFMEFeature src, String srcAttr)
	throws DataException
	{
		IFMEGeometry fmeGeom = null;
		try {
			fmeGeom = getGeometry(src, srcAttr);
			if (fmeGeom instanceof IFMEPoint) {
				IomObject coord = Fme2iox.FME2coord((IFMEPoint) fmeGeom);
				IomObject multicoord = new ch.interlis.iom_j.Iom_jObject("MULTICOORD",null);
				multicoord.addattrobj("coord", coord);
				target.addattrobj(targetAttr, multicoord);
			} else if (fmeGeom instanceof IFMEText) {
				IomObject coord = Fme2iox.FME2coord(((IFMEText) fmeGeom).getLocationAsPoint());
				IomObject multicoord = new ch.interlis.iom_j.Iom_jObject("MULTICOORD",null);
				multicoord.addattrobj("coord", coord);
				target.addattrobj(targetAttr, multicoord);
			} else if (fmeGeom instanceof IFMENull) {
				// skip it
			} else {
				throw new DataException("unexpected geometry type " + fmeGeom.getClass().getName());
			}
		} finally {
			if (fmeGeom != null) {
				fmeGeom.dispose();
			}
		}
	}
}
