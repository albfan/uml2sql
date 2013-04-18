/**
 * 
 */
package mdeServices;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.jdom.JDOMException;


import mdeServices.metamodel.Project;

import mdeServices.pythonGenerator.PythonGenerationException;

import mdeServices.transformations.TransformationNotApplicable;
import mdeServices.xmi.importXMI.ImportXMIException;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.*;
import com.googlecode.javacv.cpp.*;

import static com.googlecode.javacv.cpp.opencv_objdetect.*;



/**
 *  Main Class for generating the UMLfromPicture application 
 * 
 * @version 0.1 May 2011
 * @author jcabot
 *
 */

public class PictureToUMLGenerator extends MDEServices{
	IplImage img=null; //loadd image
	

   //different implementations may return a different number of output files (e.g. one for the domain and one for the interface)
	public Project generate(File input, String user) throws ImportXMIException, JDOMException,IOException,PythonGenerationException, TransformationNotApplicable, FileNotFoundException
	{
		
		if (o==null || l==null)
		{
			System.out.println("Configuration files not initialized");
			initialize(user);
		}
		Project p=new Project(input.getName(), user, "", "");
		//If not yet done, initializing the language resource and properties
		//Importing the model
		
		String termination=input.getPath().substring(input.getPath().length()-4,input.getPath().length());
		String saveName=input.getPath().substring(0, input.getPath().length()-4);
		String saveFinal=saveName.concat("_export")+termination;
		
		img = cvLoadImage(input.getAbsolutePath());
		int width= img.width();
		int height= img.height();
		
		//grey version of the image
		IplImage grayImage    = IplImage.create(width, height, IPL_DEPTH_8U, 1);
		cvCvtColor(img, grayImage, CV_BGR2GRAY);
		
		//reduce and double the size of the image to eliminate noise
		// could this be dangerous in my case? if the lines are too thin...
		//YES. It's not helping!
		IplImage pyr = cvCreateImage(cvSize(width/2,height/2),8,1);
		IplImage pyr2 = cvCreateImage(cvSize(width/4,height/4),8,1);
		/*cvPyrDown(grayImage, pyr,7);
		cvPyrDown(pyr, pyr2,7);
		cvPyrUp( pyr2, pyr,7);
		cvPyrUp( pyr, grayImage,7);*/
		
		 String saveGray= saveName.concat("_grey")+termination;
   	     cvSaveImage(saveGray, grayImage);
		
		
		//Running the canny detector in the figure
		IplImage cannyImage = IplImage.create(width, height, IPL_DEPTH_8U, 1);
    	/*cvCanny( grayImage, cannyImage, 0, 50, 3);
		cvDilate( cannyImage, cannyImage, null, 1 );
		String saveCanny= saveName.concat("_canny")+termination;
		 cvSaveImage(saveCanny, cannyImage);*/
		
		//Running th threshold detector
		 IplImage thresholdImage = IplImage.create(width, height, IPL_DEPTH_8U, 1);
		 //We filter all the pixels not over a 100 value in the color
		 cvThreshold( grayImage, thresholdImage, 120, 255, CV_THRESH_BINARY );
		 String saveThreshold= saveName.concat("_threshold")+termination;
		 cvSaveImage(saveThreshold, thresholdImage);
		 
		 cvCanny( thresholdImage, cannyImage, 0, 50, 3);
			cvDilate( cannyImage, cannyImage, null, 10 );
			String saveCanny= saveName.concat("_canny")+termination;
			 cvSaveImage(saveCanny, cannyImage);
		
		 //Per images fetes amb una eina, el canny em retorna les classes amb línies gruixides 
		 //el threshold quasi no canvia res ja que el color és constant a tots els pixels
		 
		 //Find contours 
		 IplImage contourImageSquare = IplImage.create(width, height, IPL_DEPTH_8U, 3);
		 IplImage contourImageTots = IplImage.create(width, height, IPL_DEPTH_8U, 3);
		 CvMemStorage storage = CvMemStorage.create();
		 CvSeq contour = new CvSeq(null);
		// cvFindContours( thresholdImage, storage, contour,  Loader.sizeof(CvContour.class),CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE );
		cvFindContours( cannyImage, storage, contour,  Loader.sizeof(CvContour.class),CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE );
		// cvFindContours( cannyImage, storage, contour,  Loader.sizeof(CvContour.class),CV_RETR_TREE, CV_CHAIN_APPROX_TC89_L1 );
		
		 IplImage thresholdImageClone = IplImage.create(width, height, IPL_DEPTH_8U, 1);
		cvThreshold( grayImage, thresholdImageClone, 120, 255, CV_THRESH_BINARY );
		
		 int ci=0;
		 while (contour != null && !contour.isNull()) {
             if (contour.elem_size() > 0) {
            	 //we only approximate the given contour, not the whole sequence
                 CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class),storage, CV_POLY_APPROX_DP, cvContourPerimeter(contour)*0.03, 0);
                 cvDrawContours(contourImageTots, points, CvScalar.BLUE, CvScalar.BLUE, -1, 1, CV_AA);
             	 if (points.total()==4)
                 {
             		 
                	 System.out.println("quadrat trobat");
             		 cvDrawContours(contourImageSquare, points, CvScalar.BLUE, CvScalar.BLUE, -1, 1, CV_AA);
                	 cvSetImageROI(thresholdImageClone, cvBoundingRect(contour,0));
                	 IplImage bwOCRImage = IplImage.create(cvSize(thresholdImageClone.roi().width(),thresholdImageClone.roi().height()), IPL_DEPTH_8U, 1);
                	 cvResize(thresholdImageClone,bwOCRImage);
                	 String saveOCRImage= saveName.concat("ocr"+ci+".tif");
                	 IplImage bigbwOCRImage = IplImage.create(cvSize(thresholdImageClone.roi().width()*2,thresholdImageClone.roi().height()*2), IPL_DEPTH_8U, 1);
                	 cvPyrUp(bwOCRImage,bigbwOCRImage,7);
                	 IplImage bigbigbwOCRImage = IplImage.create(cvSize(thresholdImageClone.roi().width()*4,thresholdImageClone.roi().height()*4), IPL_DEPTH_8U, 1);
                	 cvPyrUp(bigbwOCRImage,bigbigbwOCRImage,7);
                	 IplImage OCRImage = IplImage.create(cvSize(thresholdImageClone.roi().width()*4,thresholdImageClone.roi().height()*4), IPL_DEPTH_8U, 1);
                 	 cvThreshold( bigbigbwOCRImage,OCRImage,150, 255, CV_THRESH_BINARY );
               	     cvSaveImage(saveOCRImage, OCRImage);
                	 cvResetImageROI(grayImage);
                	 
                 }
                 
              //To see the evolution of each detected contour
              //   String saveContour= saveName.concat("_contour"+ci)+termination;
        	  //	 cvSaveImage(saveContour, contourImage);
             }
             contour = contour.h_next();
             ci=ci+1;
         }

		 String saveContour= saveName.concat("_contour")+termination;
   	     cvSaveImage(saveContour, contourImageSquare);
   	     String saveContourTots= saveName.concat("_contourtots")+termination;
   	     cvSaveImage(saveContourTots, contourImageTots);
   	    
   	     
   	     
		//Sample to smooth the image
		/* if (img!= null) {
           cvSmooth(img, img, CV_BLUR, 3);
            cvSaveImage(saveName, img);
        }*/
		
		//Finding squares
		return p;
		
	}
	
	
	//Codi que funciona per detectar classes fets amb un programa de dibuix
	/*if (o==null || l==null)
		{
			System.out.println("Configuration files not initialized");
			initialize(user);
		}
		Project p=new Project(input.getName(), user, "", "");
		//If not yet done, initializing the language resource and properties
		//Importing the model
		
		String termination=input.getPath().substring(input.getPath().length()-4,input.getPath().length());
		String saveName=input.getPath().substring(0, input.getPath().length()-4);
		String saveFinal=saveName.concat("_export")+termination;
		
		img = cvLoadImage(input.getAbsolutePath());
		int width= img.width();
		int height= img.height();
		
		//grey version of the image
		IplImage grayImage    = IplImage.create(width, height, IPL_DEPTH_8U, 1);
		cvCvtColor(img, grayImage, CV_BGR2GRAY);
		
		
		 String saveGray= saveName.concat("_grey")+termination;
   	     cvSaveImage(saveGray, grayImage);
		
		
	
		//Running th threshold detector
		 IplImage thresholdImage = IplImage.create(width, height, IPL_DEPTH_8U, 1);
		 //We filter all the pixels not over a 100 value in the color
		 cvThreshold( grayImage, thresholdImage, 120, 255, CV_THRESH_BINARY );
		 String saveThreshold= saveName.concat("_threshold")+termination;
		 cvSaveImage(saveThreshold, thresholdImage);
		
		 //Per images fetes amb una eina, el canny em retorna les classes amb línies gruixides 
		 //el threshold quasi no canvia res ja que el color és constant a tots els pixels
		 
		 //Find contours 
		 IplImage contourImageSquare = IplImage.create(width, height, IPL_DEPTH_8U, 3);
		 IplImage contourImageTots = IplImage.create(width, height, IPL_DEPTH_8U, 3);
		 CvMemStorage storage = CvMemStorage.create();
		 CvSeq contour = new CvSeq(null);
		 //cvFindContours( thresholdImage, storage, contour,  Loader.sizeof(CvContour.class),CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE );
		cvFindContours( cannyImage, storage, contour,  Loader.sizeof(CvContour.class),CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE );
		
		 int ci=0;
		 while (contour != null && !contour.isNull()) {
             if (contour.elem_size() > 0) {
            	 //we only approximate the given contour, not the whole sequence
                 CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class),storage, CV_POLY_APPROX_DP, cvContourPerimeter(contour)*0.02, 0);
                 cvDrawContours(contourImageTots, points, CvScalar.BLUE, CvScalar.BLUE, -1, 1, CV_AA);
             	 if (points.total()==4)
                 {
                	 cvDrawContours(contourImageSquare, points, CvScalar.BLUE, CvScalar.BLUE, -1, 1, CV_AA);
                	 cvSetImageROI(grayImage, cvBoundingRect(contour,0));
                	 IplImage bwOCRImage = IplImage.create(cvSize(grayImage.roi().width(),grayImage.roi().height()), IPL_DEPTH_8U, 1);
                	 cvResize(grayImage,bwOCRImage);
                	 String saveOCRImage= saveName.concat("ocr"+ci+".tif");
                	 IplImage bigbwOCRImage = IplImage.create(cvSize(grayImage.roi().width()*2,grayImage.roi().height()*2), IPL_DEPTH_8U, 1);
                	 cvPyrUp(bwOCRImage,bigbwOCRImage,7);
                	 IplImage bigbigbwOCRImage = IplImage.create(cvSize(grayImage.roi().width()*4,grayImage.roi().height()*4), IPL_DEPTH_8U, 1);
                	 cvPyrUp(bigbwOCRImage,bigbigbwOCRImage,7);
                	 IplImage OCRImage = IplImage.create(cvSize(grayImage.roi().width()*4,grayImage.roi().height()*4), IPL_DEPTH_8U, 1);
                 	 cvThreshold( bigbigbwOCRImage,OCRImage,150, 255, CV_THRESH_BINARY );
               	     cvSaveImage(saveOCRImage, OCRImage);
                	 cvResetImageROI(grayImage);
                	 
                 }
                 
              //To see the evolution of each detected contour
              //   String saveContour= saveName.concat("_contour"+ci)+termination;
        	  //	 cvSaveImage(saveContour, contourImage);
             }
             contour = contour.h_next();
             ci=ci+1;
         }

		 String saveContour= saveName.concat("_contour")+termination;
   	     cvSaveImage(saveContour, contourImageSquare);
   	     String saveContourTots= saveName.concat("_contourtots")+termination;
   	     cvSaveImage(saveContourTots, contourImageTots);
   	    
   	     
   	     
		
		
		//Finding squares
		return p;*/
		
		
			
}

