
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * This program implements Position-Aware Cuckoo Filter as well as standard
 * Cuckoo Filter and compare their results with one another in terms of False
 * Positive Rate, storage, Number of elements inserted into the filter.
 */

public class LACF {

	Bucket filter[];
	int elementsInsertFailCount;

	int popInsertedCount;
	int unpopInsertedCount;
	
	
	int unpopFalsePositiveElements;
	int popFalsePositiveElements;
	
	HashMap<Integer, Character> map = new HashMap<>();

	private final Integer Max_Kicks = ConfigLACF.MAX_KICK_OFF;
	private int filterSize = ConfigLACF.NumberofBuckets;
	String hashAlgorithm1 = ConfigLACF.HASH_ALGORITHM1;
	String hashAlgorithm2 = ConfigLACF.HASH_ALGORITHM2;
	

	static HashMap<Long, Boolean> unpopInsertedElements = new HashMap<>();
	static ArrayList<Long> unpopPositiveLookupElements = new ArrayList<Long>();

	static HashMap<Long, Boolean> popInsertedElements = new HashMap<>();
	static ArrayList<Long> popPositiveLookupElements = new ArrayList<Long>();

	/**
	 * constructor instantiating array of objects to store buckets of the filter
	 * 
	 */
	public LACF() {

		// creating 2000 filter buckets [ 0 ...1999]
		filter = new Bucket[filterSize + 1];

		// creating objects for each bucket.
		for (int i = 0; i <= filterSize; i++)
			filter[i] = new Bucket();

		popInsertedCount = 0;
		unpopInsertedCount = 0;
		unpopFalsePositiveElements = 0;
		elementsInsertFailCount = 0;
		popFalsePositiveElements = 0;

	}

	/**
	 * Each bucket contains 5 cells range from 0 to 4. cells 0 to 3 store
	 * fingerprints of elements
	 * 
	 * 
	 * <P>
	 * Method to initialize first 4 cells (0,1,2,3) of each bucket with value
	 * "128". The value "128" is chosen because in java, MD5 Hash value can
	 * range from 0 to 127. So, inorder to detect empty cell or not, value 128
	 * is chosen.
	 * 
	 */
	private void initializeFilter() {
		for (int i = 0; i <= filterSize; i++) {
			for (int j = 0; j <=9; j++) {
				filter[i].fingerprint[j] = (char) 4095;
				// initializing with 4095 to mark empty cell for a fingerprint
				// size of 12 bits.

			}

		}

	}


	/**
	 * Method to invoke hash algorithm and calculate the hash value
	 * 
	 * @param prefix
	 *            - number for which fingerprint is to be calculated.
	 * @param algorithm
	 *            - MD5 or SHA1 algorithm to calculate hash value.
	 * 
	 * @return - string hash value.
	 * 
	 */
	private String calculateHash(String prefix, String algorithm) {
		// System.out.println(prefix);
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(algorithm);

			byte[] hashedBytes = digest.digest(prefix.getBytes("UTF-8"));
			return convertByteArrtoString(hashedBytes);

		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Method to convert byte array of the hash value into string value
	 * 
	 * @param hashedVal
	 *            - byte array of hash value calculated from Hash algorithm
	 * @return - string hash value.
	 * 
	 */
	private String convertByteArrtoString(byte[] hashedVal) {

		StringBuilder build = new StringBuilder();
		for (int i = 0; i < hashedVal.length; i++) {
			build.append(Integer.toString((hashedVal[i] & 0xFF) + 0x100, 16).substring(1));
		}
		return build.toString();

	}

	/**
	 * Method which takes input number and convert it into fingerprint of char
	 * type.
	 * 
	 * @param prefix
	 *            - number to be inserted into the filter.
	 * @return - fingerprint character value.
	 */
	private char generateFingerprint(long prefix, String hashAlgorithm) {
		String hashStr = calculateHash(String.valueOf(prefix), hashAlgorithm);
		BigInteger val = new BigInteger(hashStr, 16);
		int mappedValue = val.intValue();
		// System.out.println(mappedValue + " !!!!");
		//// mappedValue = (mappedValue) % 253;
		mappedValue = (mappedValue & 0xFFF) % 4094;

		// & 0xFFFF
		if (mappedValue < 0) {
			mappedValue = mappedValue * -1;
		}
		// mappedValue += 1;

		// if (mappedValue == 0)
		// System.out.println(mappedValue + " ****");
		char fp = (char) mappedValue;

		// fpList.add(prefix + "");
		return fp;

	}

	/**
	 * Method to calculate the position of the fingerprint where it can be
	 * inserted in the filter
	 * 
	 * @param prefix
	 *            - String value for which fingerprint is to be calculated.
	 * @param algorithm
	 *            - MD5 or SHA1 algorithm to calculate hash value.
	 * 
	 * @return - bucker number in the filter.
	 */

	private int calculatePosition(String prefix, String algorithm) {
		String hash = calculateHash((prefix), algorithm);
		BigInteger val = new BigInteger(hash, 16);
		int mappedValue = val.intValue();

		mappedValue = mappedValue % filterSize;
		if (mappedValue < 0) {
			mappedValue = mappedValue * -1;
		}
		return mappedValue;

	}

	/**
	 * Method to check whether element is successfully inserted in the filter in
	 * either of the two positions .
	 * 
	 * <P>
	 * In each bucket, elements for position 1 are inserted in the following
	 * order indices: 0,1,2,3. Elements for position 2 are inserted in the
	 * following order indices : 3,2,1,0. So, position cell( cell 4) is
	 * initialized with value 3 and it decreases to 2,1,0 when the elements are
	 * inserted in position 2.
	 * 
	 * @param fp
	 *            - fingerprint of the number
	 * @param position
	 *            - position value in the filter
	 * @param hashPosition
	 *            - position 1 or position 2 of the fingerprint.
	 * 
	 * @return true - insert success. false - insert fail.
	 */
	private boolean isInsertSucess(char fp, int position) {
	
			for (int i = 0; i <= 9; i++) {
				int posVal = (int) (filter[position].fingerprint[i]);
				// System.out.println("check empty cell "+posVal);
				// checking whether it is empty cell .
				if (posVal == 4095) { // changed for 12 bits
					filter[position].fingerprint[i] = fp;
					//System.out.println(" element inserted at " + i + "  in bucket " + position);
					return true;
				}

			}
		return false;

	}

	/**
	 * Method to check whether element is inserted into the filter. If not, it
	 * will do kickOff and try to insert the element
	 * 
	 * @param fp
	 *            - fingerprint of the element
	 * @param pos1
	 *            - position 1 of the fingerprint in the filter
	 * @param pos2
	 *            - position 2 of the fingerprint in the filter
	 * 
	 * @return boolean - Insert success or failure.
	 */
	private boolean insertItem(char fp, int pos1, int pos2, String hashAlgorithm) {

		// Try to insert element in either of the two positions.
		if (isInsertSucess(fp, pos1))
			return true;
		if (isInsertSucess(fp, pos2))
			return true;

		/*
		 * Since element insertion is full, it will start KickingOFF elements.
		 * It will be done as, element from position 1 is removed ,say element
		 * "a" and current element is inserted in position 1. Position 2 of
		 * element "a" is calculated and try to insert it in the bucket. If that
		 * fails, then position 1 element from the same bucket as element "a"'s
		 * second position is removed and element "a" is inserted and its
		 * position cell is updated. Then this cycle follows till it reaches
		 * KickOFF limit.
		 */

		int elementKickedOutPos = 0;
		char tempFp = '\0';
		// choosing random position to start kick off.
		Random rand = new Random();
		int pickPos = rand.nextInt(2);
		if (pickPos == 0) {
			elementKickedOutPos = pos1;

		} else {
			elementKickedOutPos = pos2;

		}

		for (int i = 0; i < Max_Kicks; i++) {

			// choosing random position to insert the fingerprint
			//System.out.println("Kicking off at " + insertedCount);

			int randPos = rand.nextInt(4);

			tempFp = filter[elementKickedOutPos].fingerprint[randPos];
			filter[elementKickedOutPos].fingerprint[randPos] = fp;
			
			if(map.containsKey(elementKickedOutPos))
			{
				if(map.get(elementKickedOutPos).equals(tempFp))
					elementKickedOutPos += filterSize;
			}

			int nextPosOfKickedoutElement = (calculatePosition(tempFp + "", hashAlgorithm) ^ elementKickedOutPos);

			nextPosOfKickedoutElement = nextPosOfKickedoutElement % filterSize;

			if (isInsertSucess(tempFp, nextPosOfKickedoutElement))
				return true;
			else {
				fp = tempFp;
				elementKickedOutPos = nextPosOfKickedoutElement;

			}

		}

		// System.out.println("Max Kicks reached");
		return false;

	}

	private boolean insertToLacf(char fp, int pos1, int pos2, int prefixLength, long prefix, String hashAlgorithm) {
		if (insertItem(fp, pos1, pos2, hashAlgorithm)) {
			if (prefixLength == 1) {
				unpopInsertedCount++;
				unpopInsertedElements.put(prefix, true);
			} else {
				popInsertedCount++;
				popInsertedElements.put(prefix, true);
			}

			return true;

		}

		System.out.println(prefix + " not inserted at " + prefixLength);
		return false;

	}

	/**
	 * Method to calculate two positions in the filter for the number .
	 * 
	 * @param prefix
	 *            - number to be inserted into the filter.
	 * 
	 */
	private boolean insertIntoFilter(long prefix, String hashAlgorithm, int unPop) {
		char fingerprint = generateFingerprint(prefix, hashAlgorithm);
		//System.out.println(" inserting prefix " + prefix + " with fp = " + fingerprint + " length " + hashAlgorithm);
		int pos1 = -1, pos2 = -1;

		pos1 = calculatePosition(String.valueOf(prefix), hashAlgorithm);
		pos2 = (calculatePosition(fingerprint + "", hashAlgorithm)) ^ pos1;
		// calculating pos2 by XOR.

		pos2 = pos2 % filterSize;
		//System.out.println(" at pos = " + pos1 + " & " + pos2);
		
		if(pos2 > filterSize)
		{			
			pos2 = pos2 % filterSize;
			map.put(pos2, fingerprint);
			
		}

		if (!insertToLacf(fingerprint, pos1, pos2, unPop, prefix, hashAlgorithm)) {
			//System.out.println(prefix + " not inserted");
			return false;

		}
		return true;
	}

	/**
	 * Method to read input file containing random numbers and insert it into
	 * the filter
	 * 
	 * @param ipList
	 *            - List containing random numbers
	 * 
	 * 
	 */

	public void insertInputFile(List<Long> ipList, List<Integer> ipPrefixList) throws IOException {

		// Random r = new Random();
		int start = 0; // r.nextInt(10);
		for (int i = start; i < ipList.size(); i++) {
			int unpopElement = ipPrefixList.get(i);
			
			if(unpopElement == 1)
			{
				//inserting an unpopular element twice
				//using MD5 and SHA1
				//System.out.println("un pop element");
				boolean status = insertIntoFilter(ipList.get(i), hashAlgorithm2, unpopElement);
				if (!status)
					break;
				
			}
			
			boolean status = insertIntoFilter(ipList.get(i), hashAlgorithm1, unpopElement);
			
			if (!status)
				break;
		}

	}

	/**
	 * Method to check whether particular fingerprint is found in the bucket
	 * 
	 * @param fp
	 *            - fingerprint of the number
	 * @param position
	 *            - position value in the filter
	 * @param hashPosition
	 *            - position 1 or position 2 of the fingerprint.
	 * 
	 * @return true - insert success. false - insert fail.
	 */

	private boolean isIpFound(char fp, int position) {

		/*
		 * isPAModeOn : variable to denote Position Aware Mode ON. if it is
		 * True, then for position 1, it searches from 0(inclusive) till first
		 * occurence of position 2 element (exclusive)
		 * 
		 * if it is False, it can search as standard cuckoo filter, for position
		 * 1 and position 2, it searches from 0 till 3 (both inclusive).
		 */

		// checking the bucket corresponding to position 1 of the element..
		
			for (int i = 0; i <= 9; i++) { // changed for 12 bits
					if (filter[position].fingerprint[i] == fp)
						return true;
			}	
		
		return false;

	}

	/**
	 * Method to search whether the number is present in the filter
	 * 
	 * 
	 * @param prefix
	 *            - number to be searched
	 * @return boolean - True- number present, False - number not present
	 */
	private boolean searchInFilter(long prefix, String hash) {
		int pos1 = -1;
		int pos2 = -1;

		char fp = generateFingerprint(prefix, hash);
		// calculate position 1 of the element.
		pos1 = calculatePosition(String.valueOf(prefix), hash);

		// calculate position 2 of the element.
		pos2 = (calculatePosition(fp + "", hash)) ^ pos1;

		pos2 = pos2 % filterSize;

		if (isIpFound(fp, pos1))
			return true;
		if (isIpFound(fp, pos2))
			return true;
		return false;
	}

	private boolean search(long prefix, int prefixLength) {

		long ip = prefix;
		// if the element is found in the filter, then return true
		
		if(prefixLength == 1)
		{
			if (searchInFilter(ip, hashAlgorithm2)) {

				if(searchInFilter(ip, hashAlgorithm1))
					return true;
			}

			return false;
		}
		
		
		else  {
			
			if(searchInFilter(ip, hashAlgorithm1))
				return true;
		}

		return false;

	}

	private void lookup(List<String> lookupList, List<Integer> prefixList) throws IOException {

		for (int i = 0; i < lookupList.size(); i++) {

			long prefix = Integer.parseInt(lookupList.get(i));
			int prefixLength = prefixList.get(i);
			if (search(prefix, prefixLength))
			{
				if (prefixLength == 1) {
					unpopFalsePositiveElements++;
					unpopPositiveLookupElements.add(prefix);
				} else {
					popFalsePositiveElements++;
					popPositiveLookupElements.add(prefix);
				}
			}
			else {
				elementsInsertFailCount++;
			}

		}

	}

	public void printbucket() {
		for (int i = 0; i < filter.length; i++) {
			for (int j = 0; j <= 3; j++)
				System.out.print(filter[i].fingerprint[j] + "\t");

			System.out.println();

		}

	}

	/**
	 * Main method execution begins here. Note : Use PrintInsertCount.java file
	 * to run either PAFilter or standard filter. Avoid running from this main
	 * method.
	 *
	 * @throws IOException
	 *             - if the input file is not found.
	 */
	public static void main(String[] args) throws IOException {
		LACF filterObj = new LACF();

		filterObj.initializeFilter();
		System.out.println("Total inserted entries " + filterObj.unpopFalsePositiveElements);
		System.out.println("False positive count is: " + filterObj.popFalsePositiveElements);
		System.out.println("Failure Count is: " + filterObj.elementsInsertFailCount);

	}

	public int[] executeFilter(LACF filterObj, List<Long> ipList, List<Integer> ipPrefixList,
			List<String> lookupList, List<Integer> lookupPrefixList) throws IOException {
		filterObj.initializeFilter();
		//filterObj.printbucket();
		
		filterObj.insertInputFile(ipList, ipPrefixList);
		
		filterObj.lookup(lookupList, lookupPrefixList);
		
		for (long unpopPositivePrefix : unpopPositiveLookupElements) {
			if (unpopInsertedElements.containsKey(unpopPositivePrefix)) {
				unpopFalsePositiveElements--;
			}

		}

		for (long popPositivePrefix : popPositiveLookupElements) {
			if (popInsertedElements.containsKey(popPositivePrefix)) {
				popFalsePositiveElements--;
			}

		}
		
		int[] result = new int[4];
		result[0] = filterObj.unpopFalsePositiveElements;
		result[1] = filterObj.popFalsePositiveElements;
		result[2] = filterObj.popInsertedCount;
		result[3] = filterObj.unpopInsertedCount;

		System.out.println(result[0] + " fpr " + result[1]);
		System.out.println(result[3]/2 + " result " + result[2]);
		return result;

	}
}

