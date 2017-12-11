
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TestLACF {

	static ArrayList<Long> inputList = new ArrayList<Long>();
	static ArrayList<String> lookupList = new ArrayList<String>();
	static ArrayList<Integer> inputPrefixLengthList = new ArrayList<Integer>();
	static ArrayList<Integer> lookupPrefixLengthList = new ArrayList<Integer>();

	static int totalElementsInserted = 0;
	static int unpopElementsInserted = 0;

	static int totalElementsLookedup = 0;
	static int unpopElementsLookedup = 0;

	/**
	 * Method to read input file and store it in arraylist
	 * 
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public void readinputFile() throws NumberFormatException, IOException {

		BufferedReader buf = new BufferedReader(new FileReader(new File(ConfigLACF.Input_FileName)));
		String line = null;

		while ((line = buf.readLine()) != null) {

			String ip = line.replaceAll("\n", "");

			int ipPrefix = 0;

			int index = 0;

			index = ip.indexOf('\\');

			ipPrefix = Integer.parseInt(ip.substring(index + 1));
			ip = ip.substring(0, index);

			long prefix = Integer.parseInt(ip);

			inputList.add(prefix);
			totalElementsInserted++;

			// if unpopular element
			if (ipPrefix < 14 || ipPrefix > 24) {
				inputPrefixLengthList.add(1);
				unpopElementsInserted++;
			}
			// if popular element
			else {
				inputPrefixLengthList.add(0);
			}

		}

		buf.close();

		System.out.println("input file reading over");

	}

	/**
	 * Method to read lookup file and store it in arraylist
	 * 
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public void readLookupFile() throws NumberFormatException, IOException {

		BufferedReader buf = new BufferedReader(new FileReader(new File(ConfigLACF.LookUp_FileName)));

		String line = null;

		while ((line = buf.readLine()) != null) {

			String ip = line.replaceAll("\n", "");

			int ipPrefix = 0;

			int index = 0;

			index = ip.indexOf('\\');

			ipPrefix = Integer.parseInt(ip.substring(index + 1));
			ip = ip.substring(0, index);

			lookupList.add(ip);
			totalElementsLookedup++;

			// if unpopular element
			if (ipPrefix < 14 || ipPrefix > 24) {
				lookupPrefixLengthList.add(1);
				unpopElementsLookedup++;
			}
			// if popular element
			else {
				lookupPrefixLengthList.add(0);
			}

		}
		buf.close();

		System.out.println("lookup file reading over");

	}

	/**
	 * Method to execute the filter and store the result into arraylist.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void executeFilter() throws IOException, InterruptedException {

		// number of times filter has to be executed.
		int loopCount = 1;

		List<Integer> unpopFPElements = new ArrayList<Integer>();
		List<Integer> popFPElements = new ArrayList<Integer>();
		List<Integer> popFilterOccupancy = new ArrayList<Integer>();
		List<Integer> unpopFilterOccupancy = new ArrayList<Integer>();

		for (int i = 0; i < loopCount; i++) {
			LACF obj = new LACF();

			int[] result = obj.executeFilter(obj, inputList, inputPrefixLengthList, lookupList, lookupPrefixLengthList);
			// int[] result = obj.executeFilter(obj, inputList,
			// inputPrefixLengthList, inputList, inputPrefixLengthList);

			unpopFPElements.add(result[0]);
			popFPElements.add(result[1]);
			popFilterOccupancy.add(result[2]);
			unpopFilterOccupancy.add(result[3]);

			obj = null;

		}

		int avgUnpopFPElements = 0;
		int avgPopFPElements = 0;
		int averagePopFilterOccupancy = 0;
		int averageUnpopFilterOccupancy = 0;

		for (int i = 0; i < loopCount; i++) {
			avgUnpopFPElements += unpopFPElements.get(i);
			avgPopFPElements += popFPElements.get(i);
			averagePopFilterOccupancy += popFilterOccupancy.get(i);
			averageUnpopFilterOccupancy += unpopFilterOccupancy.get(i);

		}

		avgUnpopFPElements = avgUnpopFPElements / loopCount;
		avgPopFPElements = avgPopFPElements / loopCount;
		averagePopFilterOccupancy = averagePopFilterOccupancy / loopCount;
		averageUnpopFilterOccupancy = averageUnpopFilterOccupancy / loopCount;
		/*
		 * System.out.println("-----"); System.out.println(avgPopFPElements);
		 * System.out.println(avgUnpopFPElements); System.out.println("-----");
		 * System.out.println(averagePopFilterOccupancy);
		 * System.out.println(averageUnpopFilterOccupancy);
		 * System.out.println("-----");
		 */

		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(4);

		System.out.println();
		double o = (float) (averagePopFilterOccupancy + averageUnpopFilterOccupancy)
				/ (ConfigLACF.NumberofBuckets * 10);
		int f = 12;

		// averageUnpopFilterOccupancy /= 2;
		System.out.println("filter occupancy percentage = " + o);
		float u = (float) averageUnpopFilterOccupancy
				/ (float) (averageUnpopFilterOccupancy + averagePopFilterOccupancy);
		System.out.println("Unpop fraction = " + u);

		double programUnpopFPR = (float) (avgUnpopFPElements) / unpopElementsLookedup;
		programUnpopFPR *= 100;
		double programPopFPR = (float) (avgPopFPElements) / (totalElementsLookedup - unpopElementsLookedup);
		programPopFPR *= 100;

		double programFPR = (float) (avgPopFPElements + avgUnpopFPElements) / totalElementsLookedup;
		programFPR *= 100;

		System.out.println();
		System.out.println("-- Program --  " + df.format(programFPR));
		System.out.println(" fpr unpop = " + df.format(programUnpopFPR) + "\t fpr pop = " + df.format(programPopFPR));

		double formulaUnpopFPR = (float) (8 * o * u) / Math.pow(2, f);
		formulaUnpopFPR *= 100;
		double formulaPopFPR = (float) (8 * o * (1 - u)) / Math.pow(2, f);
		formulaPopFPR *= 100;

		System.out.println();
		System.out.println("-- Formula --");
		// System.out.println(" fpr unpop = " + df.format(formulaUnpopFPR) + "\t
		// fpr pop = " + df.format(formulaPopFPR));
		System.out.println(" fpr unpop = " + (formulaUnpopFPR) + "\t fpr pop = " + (formulaPopFPR));

	}

	/**
	 * Main method starts here
	 * 
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {

		TestLACF p1 = new TestLACF();
		p1.readinputFile();
		p1.readLookupFile();
		p1.executeFilter();

	}

}
