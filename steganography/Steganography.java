// 10/23/14 @ 12:42am | Assignment complete

import java.io.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.*;
public class Steganography {
	private enum Mode {
    	ENCODE,	DECODE
    }
    private static boolean verbose = false;

	private static FileInputStream inMessage;
    private static FileOutputStream outMessage;
    
    private static Mode mode;

    private static BufferedImage image;

    private static int[] bytes;
    private static int[][] pixels;
    private static int
    	imgHeight, imgWidth, totalPixels, currentPixel, currentIndex, currentByte;

	public static void main(String[] args) {
		// Check if the arguments are valid
		checkArgs(args);

		getMode(args[0]);

		// Get file's extension, check if valid
		String extension = getExtension(args[1]);

		// Get file's name
		String fileName = getName(args[1]);

		// Create BufferedImage from file name
		image = getImage(args[1]);

		// Print statistics
        printImageStats(image, args[1]);

        // Create an array to store the image's pixels
       	createPixelArray(image);

       	/*
	       	Create an array to store the bytes of the message.
	       	Initialized to the maximum size, which is totalPixels*3
	       	since we are storing 3 bits per pixel. Initializing all
	       	values to -1 to show where the message ends.
       	*/
       	bytes = new int[totalPixels*3];
       	for (int i = 0; i<bytes.length;i++)
       		bytes[i] = -1;

       	// Encode/Decode the file
       	switch (mode){
       		case DECODE: 
       			createOutputMessage(args[2]);
       			decodePixels();
       			closeOutput();
       			break;
       		case ENCODE:
       			createInputMessage(args[2]);
       			getBitsFromMessage();
       			encodePixels();
       			replaceImagePixels(image);
				writeImage(image, fileName, extension);
				closeInput();
       			break;
       	}
       	if (verbose)
			System.out.println("Process complete!");
	}
	private static void decodePixels(){
		/*
			Iterate through the array of pixels
			and evaluate whether the stored bit is
			a 0 or 1 based on whether it is even/odd
			(0 even, 1 odd). Iterate through the array
			until the last pixel is reached, or the
			zero byte, which represents the end of
			the message.
		*/

		if (verbose)
			System.out.print("Decoding pixels...");
		int pix = 0;
		int index = 0;
		int count = 0;
		byte byteArray[] = new byte[8];
		while (pix<totalPixels&&pixels[pix][index]!=0){
			byteArray[count] = (pixels[pix][index]%2==0)?(byte)0:(byte)1;
			if (++count==8){
				count = 0;
				int result = 0;
				for (int i = 0; i<8; i++){
					result |= (byteArray[i] << (7-i));
				}
				try{
					outMessage.write(result);
				} catch(IOException e){
					System.out.println("unable to write byte");
				}
			}
			if (++index>2){
				index = 0;
				pix++;
			}
		}
		if (verbose)
			System.out.println("done!");
	}

	private static void getBitsFromMessage(){
		/*
			Create an array of bits, reading from the
			message file specified in the command line.
			This array of bits represents each bit in
			the message. The bits will later be stored
			into the array of pixels.
		*/
		if (verbose)
			System.out.print("Creating bit array...");
		try{
			int byteRead;
			while(currentByte<bytes.length&&(byteRead = inMessage.read()) != -1){
				for (int i = 0; i<8; i++){
					if (currentByte>=bytes.length)
						break;
					bytes[currentByte++] = (byteRead >> (7-i))&1;
				}
			}
			if (currentByte>=bytes.length)
				System.out.println("(warning) Message truncated.");
		} catch(IOException e) {
			System.out.println("(Error) Unable to read file!");
			return;
		}
		if (verbose)
			System.out.println("done!");
	}
	private static boolean hasNextPixel(){
		// Checks to see if there are more pixels.
		// Returns false if the current pixel is the last pixel.

		if (currentPixel+1<totalPixels)
			return true;
		if (currentPixel+1==totalPixels&&currentIndex<2)
			return true;
		return false;
	}
	private static void setCurrentPixelValue(int value){
		// Replaces the value of the current pixel, and moves the pointer forward

		pixels[currentPixel][currentIndex]=value;
		if (++currentIndex>2){
			currentIndex = 0;
			currentPixel++;
		}
	}
	private static int getCurrentPixel(){
		// Returns the current pixel in the array

		return pixels[currentPixel][currentIndex];
	}
	private static void encodePixels(){
		/*
			Iterates through the array of bits that are to be
			stored and changes the pixels in the array to even
			or odd, depending on the bit. (0 even, 1 odd)
		*/

		if (verbose)
			System.out.print("Encoding pixels...");
		currentByte = 0;
		while (hasNextPixel()){
			if (bytes[currentByte]==-1){
				break;
			}
			switch(bytes[currentByte]){
				case(1): setCurrentPixelValue(makeOdd(getCurrentPixel())); break;
				case(0): setCurrentPixelValue(makeEven(getCurrentPixel())); break;
			}
			currentByte++;
		}
		setCurrentPixelValue(0);
		if (verbose)
			System.out.println("done!");
	}
	private static int makeEven(int x){
		// Make the given int even

		if ((x%2)==0)
			return x;
		if (x==255)
			return x-1;
		return x+1;
	}
	private static int makeOdd(int x){
		// Make the given int odd

		if ((x%2)!=0)
			return x;
		return x+1;
	}
	private static void createPixelArray(BufferedImage img){
		// Create an array of pixels obtained from the BufferedImage

		if (verbose)
			System.out.print("creating pixel array...");
		pixels = new int[totalPixels][];
		int x, y;
		x=y=0;
		for (int i = 0; i<totalPixels;i++){
			int rgb = img.getRGB(x, y);
			
			pixels[i] = new int[]{
				(int)((rgb>>16) & 0xff), // Red Value
	    		(int)((rgb>>8) & 0xff), // Green Value
	    		(int)(rgb & 0xff)  // Blue value
    		};
			if ((++x)==imgWidth){
				x = 0;
				y++;
			}
		}
		if (verbose)
			System.out.println("done!");
	}
	private static void writeImage(BufferedImage img, String name, String ext){
		// Write the BufferedImage to a file on the disk

		try{
			File outputfile = new File(name+"-steg"+ext);
			ImageIO.write(img, ext.substring(1,ext.length()), outputfile);
		} catch (IOException e){}
	}
	private static void replaceImagePixels(BufferedImage img){
		/*
			Replace the pixels of the BufferedImage
			with the new values created from the even/odd
			encoding method	
		*/ 

		if (verbose)
			System.out.print("replacing pixels...");
		int x, y;
		x=y=0;
		for (int i = 0; i<totalPixels;i++){
			int red,blue,green;
			red = pixels[i][0];
			green = pixels[i][1];
			blue = pixels[i][2];
    		int rgb = (red<<16)|(green<<8)|(blue);
    		img.setRGB(x, y, rgb);

			if ((++x)==imgWidth){
				x = 0;
				y++;
			}
		}
		if (verbose)
			System.out.println("done!");
	}

	private static BufferedImage getImage(String args){
		// Open the file and store it in a BufferedImage data structure

		BufferedImage img = null;
        try {
            img = ImageIO.read(new File(args));
        } catch (IOException e) {
        	System.out.printf("Unable to open %s!\n",args);
        	System.exit(-1);
        }
        return img;
	}
	private static void printImageStats(BufferedImage img, String args){
		// Print image statistics. Height, Width, Total pixels

        imgHeight = img.getHeight();
        imgWidth = img.getWidth();
        totalPixels = imgHeight*imgWidth;
        currentPixel = currentIndex = currentByte = 0;

        System.out.printf("Name: %s\n",args);
        System.out.printf("Pixels: %d\n",imgWidth * imgHeight);
       	System.out.printf("imgHeight: %d\n",imgHeight);
       	System.out.printf("imgWidth: %d\n",imgWidth);
       	System.out.println();
	}
	private static void createInputMessage(String args){
		try{
			// Create an input and output stream for the inMessage/out files
			inMessage = new FileInputStream(args);
		} catch(FileNotFoundException e) {
			// Return an error if unable to do so.
			System.out.println("(Error) Unable to open input message.");
			System.exit(-1);
		}
		if (verbose)
			System.out.printf("Reading message from '%s'\n",args);
	}
	private static void createOutputMessage(String name){
		try{
			// Create an input and output stream for the inMessage/out files
			outMessage = new FileOutputStream(name);
		} catch(FileNotFoundException e) {
			// Return an error if unable to do so.
			System.out.println("(Error) Unable to create output message.");
			System.exit(-1);
		}
		System.out.printf("Output file '%s' created successfully!\n",name);
	}
	private static void closeInput(){
		try{
			// Close the input/output streams
			inMessage.close();
		} catch(IOException e) {
			// Return an error if unsuccessful
			// This is unexpected.
			System.out.println("(Error) Unable to close input!");
			System.exit(-1);
		}
	}
	private static void closeOutput(){
		try{
			// Close the input/output streams
			outMessage.close();
		} catch(IOException e) {
			// Return an error if unsuccessful
			// This is unexpected.
			System.out.println("(Error) Unable to close output!");
			System.exit(-1);
		}
	}
	private static void getMode(String args){
		System.out.print("Mode: ");
		if ("-e".equalsIgnoreCase(args)){
			// Steganography -E image.png my-message
			mode = Mode.ENCODE;
			System.out.println("encode");
		}
		else if ("-d".equalsIgnoreCase(args)) {
			// Steganography -D image-steg.png my-message-out
			mode = Mode.DECODE;
			System.out.println("decode");
		}
		else{
			System.out.println("Unkown mode entered.");
			return;
		}
	}
	private static String getExtension(String fileName){
		String extension = null;
		for (int i = fileName.length()-1; i>0; i--) {
			if (!(fileName.charAt(i)== '.'))
				continue;
			extension = fileName.substring(i,fileName.length());
			break;
		}
		if (!extension.equalsIgnoreCase(".jpg")&&!extension.equalsIgnoreCase(".jpeg")&&!extension.equalsIgnoreCase(".png")&&!extension.equalsIgnoreCase(".bmp"))
			extension = null;
		if (extension==null){
			// Invalid
			System.out.printf("(Error) %s is not of a supported file type.\n",fileName);
			System.exit(-1);
		}
		return extension;
	}
	private static String getName(String fileName){
		String name = null;
		for (int i = fileName.length()-1; i>0; i--) {
			if (!(fileName.charAt(i)== '.'))
				continue;
			name = fileName.substring(0,i);
			break;
		}
		if (name==null)
			return null;
		return name;
	}
	private static void checkArgs(String[] args){
		if (args.length!=3||!("-e".equalsIgnoreCase(args[0])||"-d".equalsIgnoreCase(args[0]))){
			/*
				Flags: 
				Steganography -E image.png my-message
				Steganography -D image-steg.png my-message-out
			*/
			System.out.println("(Error) Usage: java Steganography <E/D> <inputFile> <message>");
			System.exit(-1);
		}
	}
}