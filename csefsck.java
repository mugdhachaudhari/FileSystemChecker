import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class csefsck {
	
	
	public static String pathStr = "FS";
	public static String fileStart = "fusedata.";
	public static int deviceId = 20;
	public static int superBlockN = 0;
	public static int blockSize = 4096;

	
	
	
	private static String returnPath()
	{
		URL url = Thread.currentThread().getContextClassLoader().getResource(pathStr);
		String FileName = url.getPath() + java.io.File.separator + fileStart;
		return FileName;
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		ArrayList<Integer> occupiedBlks = new ArrayList<Integer>();
		ArrayList<Integer> availableBlks = new ArrayList<Integer>();
		ArrayList<Integer> freeBlks = new ArrayList<Integer>();
		
		
		SuperBlock sb = readSuperBlock(superBlockN);

		validateSuperBlock(sb);

		occupiedBlks.add(superBlockN);
		for (int i = sb.freeStart; i <= sb.freeEnd; i++) {
			occupiedBlks.add(i);
		}

		for (int i = 0; i < sb.maxBlocks; i++) {
			availableBlks.add(i);
		}

		freeBlksList(sb, freeBlks);

		validateFilesDirectories(sb.root, sb.root, occupiedBlks, freeBlks);
		validateFreeBlkList(availableBlks, freeBlks, occupiedBlks, sb);

		System.out.println("File system checking completed ");
			
		
	}




	
	private static StringBuilder readFile(int j)
	{

		StringBuilder sb = new StringBuilder();
		//Path path = Paths.get("FS");
		String FileName = returnPath() + j;
		String line = null;
		
		try
		{			
			BufferedReader br = new BufferedReader(new FileReader(FileName));
			
			while ((line = br.readLine()) != null)
			{
				sb.append(line);
			}
			//	System.out.println(sb);
			br.close();
			
		}
		
		catch (FileNotFoundException e)
		{
			System.out.println("Unable to open file " + FileName);
			System.exit(1);
			
		}
		
		catch (IOException e)
		{
			System.exit(1);
		}
		
		finally
		{
			return sb;
		}
	}
	
	private static void writeFile(String s, int num)
	{
		String FileName = returnPath() + num;
		try
		{
//			System.out.println(s);
			BufferedWriter bw = new BufferedWriter(new FileWriter(FileName));
			bw.write(s);
			bw.close();
		}
		
		catch (FileNotFoundException e)
		{
			System.out.println("Unable to write file " + FileName);
			System.exit(1);
			
		}
		
		catch (IOException e)
		{
			System.exit(1);
		}
		
	}
	
	private static boolean validateDate(long creationTime)
	{
		Date curDate = new Date();
		Date fDate = new Date(creationTime);
//		System.out.println(curDate);
//		System.out.println(fDate);
		if (fDate.compareTo(curDate) > 0)
			return true;
		return false;
	}
	
	
	private static HashMap<String, String> KvData(StringBuilder s)
	{
		HashMap<String, String> kvHash = new HashMap<String, String>();
		
		s.deleteCharAt(0);
		s.deleteCharAt(s.length() - 1);
				
		for (String att : s.toString().split(","))
		{
			String[] kv = att.split(":");
			kvHash.put(kv[0].trim(), kv[1].trim());
		}
		return kvHash;
		
	}
	
	
	private static void validateSuperBlock(SuperBlock sb)
	{
		boolean isChanged;
		if (validateDate(sb.creationTime) ||  sb.devId != deviceId)
		{
			if (validateDate(sb.creationTime))
			{
				sb.creationTime = (new Date()).getTime();
				System.out.println("Notification 2 : Modified creation time of FileSystem");
			}
			if(sb.devId != deviceId)
			{
				sb.devId = deviceId;
				System.out.println("Notification 1 : DeviceId corrected" );
			}
			
			
			writeFile(sb.toString(), superBlockN);
			
		}
		else
			System.out.println("Notification 2 : SuperBlock deviceId  and creationDate Validated");
	}
	
	private static SuperBlock readSuperBlock(int num)
	{

		StringBuilder s = readFile(num);
		HashMap<String, String> sbData = KvData(s);
		
		SuperBlock sb = new SuperBlock(sbData.get("creationTime"),
				sbData.get("mounted"), sbData.get("devId"),
				sbData.get("freeStart"), sbData.get("freeEnd"),
				sbData.get("root"), sbData.get("maxBlocks"));
		return sb;
			
		
	}
	
	private static void validateFiles(int loc, ArrayList<Integer> occBlks, ArrayList<Integer> freeBlks)
	{
		
		StringBuilder s = readFile(loc);
//		System.out.println(s);
		
		occBlks.add(loc);
		
		HashMap<String, String> fileData = new HashMap<String, String>();
		
		s.deleteCharAt(0);
		s.deleteCharAt(s.length() - 1);
		
		String md = "indirect";
		int i = s.indexOf(md);
		StringBuilder fs = new StringBuilder(s.substring(0, i - 1));
		StringBuilder ls = new StringBuilder(s.substring(i, s.length()));
		String fss = fs.toString().trim();
		String lss = ls.toString().trim();
		fs = new StringBuilder(fss);
		ls = new StringBuilder(lss);
		
		fs.deleteCharAt(fs.length() - 1);
//		ls.deleteCharAt(ls.length() - 1);
//		System.out.println("File Split up");
//		System.out.println(fs);
//		System.out.println(ls);
		
		for (String att : fs.toString().split(","))
		{
			String[] kv = att.split(":");
			fileData.put(kv[0].trim(), kv[1].trim());
		}
		
		for (String att : ls.toString().split(" "))
		{
			String[] kv = att.split(":");
			fileData.put(kv[0].trim(), kv[1].trim());
		}
		
		
		File fl = new File(fileData.get("size"), fileData.get("uid"), fileData.get("gid"), fileData.get("mode"), fileData.get("linkcount"),
		fileData.get("atime"), fileData.get("ctime"), fileData.get("mtime"), fileData.get("indirect"), fileData.get("location"));
//		System.out.println("Checking File" + loc);
//		System.out.println(fl);
				
		FileValidation fileV = new FileValidation();
		validateFTime(fl.atime, fl.ctime, fl.mtime, fileV, fl);
		
//		Check if location pointer is an array when indirect is 1.
		
		validateLocIndirect(fl, loc, fileV, occBlks, freeBlks);
		
//		System.out.println(fl);
		if(fileV.isatM || fileV.isctM || fileV.ismtM || fileV.isIndirct || fileV.isLoc)
		{
			writeFile(fl.toString(), loc);
			if(fileV.isatM)
				System.out.println("Notification 2 : Access time has been corrected for file " + loc);
			if(fileV.isctM)
				System.out.println("Notification 2 : Creation time has been corrected for file " + loc);
			if(fileV.ismtM)
				System.out.println("Notification 2 : Modified time has been corrected for file " + loc);
			if(fileV.isIndirct)
				System.out.println("Notification 6 : Indirection has been corrected for file " + loc);
			if(fileV.isLoc)
				System.out.println("Notification 6 : Location has been corrected for file " + loc);
			
		}
//		System.out.println("Corrected File" + loc);
//		System.out.println(fl);
		
	}
	
	private static boolean isInteger(String s)
	{
		try{
			Integer.parseInt(s);
			return true;
		}
		
		catch(Exception e)
		{
			return false;
		}
	}
	
	
	private static void validateLocIndirect(File fl, int fileNum, FileValidation fileV, ArrayList<Integer> occBlks, ArrayList<Integer> freeBlks)
	{
		boolean isValidIndirect = true;
		StringBuilder lc = readFile(fl.location);
		occBlks.add(fl.location);
		ArrayList<Integer> locArray = new ArrayList<Integer>();
		for (String att : lc.toString().split(","))
		{
			if(isInteger(att.trim()))
			{
				locArray.add(Integer.parseInt(att.trim()));
			}
			else
			{
				isValidIndirect = false;
				break;
			}
			
		}
		
		if (fl.indirect == 0 && isValidIndirect)
		{
			fl.indirect = 1;
			fileV.isIndirct = true;
//			System.out.println("Indirect changed");
		}
		else if (fl.indirect == 1 && !isValidIndirect)
		{
			fl.indirect = 0;
			fileV.isIndirct = true;
//			System.out.println("Indirect changed");
		}
		
		if (fl.indirect == 1)
		{
			occBlks.addAll(locArray);
		}
		
//		System.out.println("Indirect Validation " + fl);
//		Validate size of file
		validateSizeofFile(fl, fileNum, fileV, locArray, freeBlks, occBlks);
		

	}
	
	private static void validateSizeofFile(File fl, int fileNum, FileValidation fileV, ArrayList<Integer> locArray, ArrayList<Integer> freeBlks, ArrayList<Integer> occBlks)
	{
		if (fl.indirect == 0)
		{
			StringBuilder s = readFile(fl.location);
			if (fl.size != s.length() && s.length() <= blockSize)
			{
				fl.size = s.length();
				System.out.println("Notification 7 : File size has been corrected for file " + fileNum);
			}
		}
		
		
		else if (fl.indirect == 1)
		{
			StringBuilder fileData = new StringBuilder();
			int fileSize = 0;
			
			for(Integer dataBlkLoc: locArray)
			{
				StringBuilder sd = readFile(dataBlkLoc);
				fileData.append(sd);
				fileSize+=sd.length();
			}
			
			if (fl.size != fileSize)
			{
				fl.size = fileSize;
				System.out.println("Notification 7 : File size has been corrected for file " + fileNum);
			}
			
			int iArrayCnt = (int) Math.ceil(fileSize/((double) blockSize));
			
			if ((iArrayCnt == 1 && fileSize <= blockSize) || (iArrayCnt == 1 && fileSize%blockSize == 0))
			{
				fl.indirect = 0;
				fileV.isLoc = true;
				fileV.isIndirct = true;
				int indexBlk = fl.location;
				fl.location = locArray.get(0);
				fileV.isIndirct = true;
				fileV.isLoc = true;
				writeFile(fileData.toString(), fl.location);
				locArray.remove(0);
				addFreeBlks(locArray, indexBlk, freeBlks);
				locArray.add(indexBlk);
				remOccBLks(locArray, occBlks);
				System.out.println("Notification 7 : No need of indirection for file " + fileNum + ". Index block " + indexBlk +" moved to free block list and file location pointed to data block after changing indirect to 0.");
				
			}
			else
			{
				if(iArrayCnt < locArray.size())
				{
					ArrayList<Integer> usedBlks = new ArrayList<Integer>();
					for (int i = 0; i < iArrayCnt; i++)
					{
						int st = i*blockSize;
						int end = (i + 1)*blockSize;
						writeFile(fileData.substring(st, end), locArray.get(0));
						usedBlks.add(locArray.get(0));
						locArray.remove(0);
					}
					
					addFreeBlks(locArray, 0, freeBlks);
					remOccBLks(locArray, occBlks);
					
					StringBuilder newIndirectBlk = new StringBuilder();
					for (int ub: usedBlks)
						newIndirectBlk.append(ub+",");
						
					writeFile(newIndirectBlk.substring(0, newIndirectBlk.length() -2),fl.location);
					
					System.out.println("Notification 7 : More than required index block pointers used for file " + fileNum +". Additional index pointers " + locArray +"moved to free block list");
				}
			}
			
		}
		
	}
	
	private static void addFreeBlks( ArrayList<Integer> locArray, int indexBlk,  ArrayList<Integer> freeBlks)
	{
		if(!locArray.isEmpty())
			freeBlks.addAll(locArray);
		if(indexBlk != 0)
			freeBlks.add(indexBlk);
		
	}
	
	private static void remOccBLks(ArrayList<Integer> locArray, ArrayList<Integer> occBlks)
	{
		if(!locArray.isEmpty())
			occBlks.removeAll(locArray);
	}
	
	
	private static void validateDirList(StringBuilder s, int dirNum, int pDirNum, DirValidation dirV, Directory dir, ArrayList<Integer> occBlks, ArrayList<Integer> freeBlks)
	{		
		
//		boolean isDtP, isDtDtP;
		HashMap<String, String> dirHm = new HashMap<String, String>();
		HashMap<String, String> flHm = new HashMap<String, String>();
		HashMap<String, String> spHm = new HashMap<String, String>();
		StringBuilder newLs = new StringBuilder();
		newLs.append("{");
		int inodeCnt = 0;
		
		for (String att : s.toString().split(","))
		{
			String[] kv = att.split(":");
			if (kv[0].trim().equals("d"))
			{
//				System.out.println("Check Trim" + kv[0].trim());
				dirHm.put(kv[1].trim(), kv[2].trim());
				if (!kv[1].trim().equals(".") && !kv[1].trim().equals(".."))
				{
					inodeCnt++;
					newLs.append(att); newLs.append(", ");
//					System.out.println("D");
//					System.out.println(inodeCnt);
//					System.out.println(newLs);
					validateFilesDirectories(Integer.parseInt(kv[2].trim()), dirNum, occBlks, freeBlks);
				}
				else
				{
					inodeCnt++;
					if (kv[1].trim().equals("."))
					{
						validateCurPtDir(newLs, dirV, dirNum, ".", Integer.parseInt(kv[2]));
//						System.out.println(".");
//						System.out.println(inodeCnt);
//						System.out.println(newLs);
					}
					else 
						{
						validateCurPtDir(newLs, dirV, pDirNum, ".." , Integer.parseInt(kv[2].trim()));
//						System.out.println("..");
//						System.out.println(inodeCnt);
//						System.out.println(newLs);
						}
					
				}
				
			}
			else if (kv[0].trim().equals("f"))
			{
				inodeCnt++;
				flHm.put(kv[1].trim(), kv[2].trim());
				newLs.append(att); newLs.append(", ");
//				System.out.println("Validating Files");
				validateFiles(Integer.parseInt(kv[2].trim()), occBlks, freeBlks);
//				System.out.println("F");
//				System.out.println(inodeCnt);
//				System.out.println(newLs);
			}
			else if  (kv[0].trim().equals("s"))
			{
				inodeCnt++;
				newLs.append(att); newLs.append(", ");
				spHm.put(kv[1].trim(), kv[2].trim());
//				System.out.println("S");
//				System.out.println(inodeCnt);
//				System.out.println(newLs);
			}
			

		}
		
		if (!dirHm.containsKey("."))
		{
			dirV.isDtP = true;
			addCPDir(".", dirNum, newLs);
		}
		else if (!dirHm.containsKey(".."))
		{
			dirV.isDtP = true;
			addCPDir("..", pDirNum, newLs);
		}
		
//		Each directory’s link count matches the number of links in the filename_to_inode_dict
		if (dir.linkcount != inodeCnt)
		{
//			System.out.println("Dir Linkcount " + dir.linkcount);
			dirV.isLnkcM = true;
			dir.linkcount = inodeCnt;
//			System.out.println("Dir Linkcount after " + dir.linkcount);
		}
		
		
		//CHANGE LS OF DIRECTORY HERE IF REQUIRED
		if (dirV.isDtP || dirV.isDtDtP)
		{
			newLs.deleteCharAt(newLs.length() - 2);
			newLs.append("}");
			dir.filename_to_inode_dict = newLs.toString();
		}
		
//		System.out.println(s);
		
		
	}
	
	private static void addCPDir(String s, int num, StringBuilder newLs)
	{
		newLs.append("d:");
		newLs.append(s);
		newLs.append(":");
		newLs.append(Integer.toString(num));
		newLs.append(", ");
	}
	
	private static void validateCurPtDir(StringBuilder newLs, DirValidation dirV, int num, String s, int fnum)
	{
		if (num != fnum)
		{
			if(s.equals("."))
				dirV.isDtP = true;
			if(s.equals(".."))
				dirV.isDtDtP = true;
		}
		
		newLs.append("d:");
		newLs.append(s);
		newLs.append(":");
		newLs.append(Integer.toString(num));
		newLs.append(", ");
		
	}
	
	private static void validateDTime(long atime, long ctime, long mtime, DirValidation dirV, Directory dir)
	{
		boolean ate = validateDate(dir.atime);
		boolean ct = validateDate(dir.ctime);
		boolean mt = validateDate(dir.mtime);
		

			if (ate)
			{
				dirV.isatM = true;
				dir.atime = (new Date()).getTime();
			}
			if (ct)	 
			{
				dirV.isctM = true;
				dir.ctime = (new Date()).getTime();
			}
			if (mt)	 
			{
				dirV.ismtM = true;
				dir.mtime = (new Date()).getTime();
			}

		
	}
	
	
	private static void validateFTime(long atime, long ctime, long mtime, FileValidation fileV, File fl)
	{
		boolean ate = validateDate(fl.atime);
		boolean ct = validateDate(fl.ctime);
		boolean mt = validateDate(fl.mtime);
		
			if (ate)
			{
				fileV.isatM = true;
				fl.atime = (new Date()).getTime();
			}
			if (ct)	 
			{
				fileV.isctM = true;
				fl.ctime = (new Date()).getTime();
			}
			if (mt)	 
			{
				fileV.ismtM = true;
				fl.mtime = (new Date()).getTime();
			}

		
	}
	
	private static void validateFilesDirectories(int dirNum, int pDirNum, ArrayList<Integer> occBlks, ArrayList<Integer> freeBlks)
	{
		occBlks.add(dirNum);
		StringBuilder s = readFile(dirNum);
//		System.out.println(s);
		
		HashMap<String, String> dirData = new HashMap<String, String>();
		
		s.deleteCharAt(0);
		s.deleteCharAt(s.length() - 1);
		
		String md = "filename_to_inode_dict:";
		int i = s.indexOf(md);
		StringBuilder fs = new StringBuilder(s.substring(0, i - 1));
		StringBuilder ls = new StringBuilder(s.substring(i + md.length(), s.length()));
		String fss = fs.toString().trim();
		String lss = ls.toString().trim();
		fs = new StringBuilder(fss);
		ls = new StringBuilder(lss);
		
		dirData.put(md, ls.toString());
		fs.deleteCharAt(fs.length() - 1);
		ls.deleteCharAt(0);
		ls.deleteCharAt(ls.length() - 1);
		
//		System.out.println(fs);
//		System.out.println(ls);
		
		for (String att : fs.toString().split(","))
		{
			String[] kv = att.split(":");
			dirData.put(kv[0].trim(), kv[1].trim());
		}
		
		Directory dir = new Directory(dirData.get("size"), dirData.get("uid"), dirData.get("gid"), dirData.get("mode"), 
				dirData.get("atime"), dirData.get("ctime"), dirData.get("mtime"), dirData.get("linkcount"), dirData.get("filename_to_inode_dict:"));

//		System.out.println("Checking below Directory" + dirNum);
//		System.out.println(dir);
		
		DirValidation dirV = new DirValidation();
		validateDirList(ls, dirNum, pDirNum, dirV, dir, occBlks, freeBlks);
		validateDTime(dir.atime, dir.ctime, dir.mtime, dirV, dir);
		
		if (dirV.isatM || dirV.isctM || dirV.isLnkcM || dirV.ismtM || dirV.isDtP || dirV.isDtDtP)
		{
			writeFile(dir.toString(), dirNum);
			if(dirV.isatM)
				System.out.println("Notification 2 : Access time has been corrected for directory " + dirNum);
			if(dirV.isctM)
				System.out.println("Notification 2 : Creation time has been corrected for directory " + dirNum);
			if(dirV.ismtM)
				System.out.println("Notification 2 : Modified time has been corrected for directory " + dirNum);
			if(dirV.isDtP)
				System.out.println("Notification 4 : Current directory has been corrected for directory " + dirNum);
			if(dirV.isDtDtP)
				System.out.println("Notification 4 : Parent directory has been corrected for directory " + dirNum);
			if(dirV.isLnkcM)
				System.out.println("Notification 5 : LinkCount has been corrected for directory " + dirNum);
			
		}
//		System.out.println("Changed Directory" + dirNum);
//		System.out.println(dir);

		
		
	}
	
	private static void freeBlksList(SuperBlock sb, ArrayList<Integer> freeBlks)
	{
		for (int i = sb.freeStart; i <= sb.freeEnd; i++)
		{
			StringBuilder lb = readFile(i);
			for (String att : lb.toString().split(","))
			{
				freeBlks.add(Integer.parseInt(att.trim()));
			}
		}
	}
	
	
	private static void validateFreeBlkList(ArrayList<Integer> availableBlks, ArrayList<Integer> freeBlks, ArrayList<Integer> occupiedBlks, SuperBlock sb)
	{
		
//		System.out.println(availableBlks);
//		System.out.println(occupiedBlks);
//		System.out.println(freeBlks);
//		System.out.println(availableBlks.size());
//		System.out.println(occupiedBlks.size());
//		System.out.println(freeBlks.size());
		
		boolean isChanged = false;
		ArrayList<Integer> occAsperFreeBlks = new ArrayList<Integer>(availableBlks);
		occAsperFreeBlks.removeAll(freeBlks);
//		System.out.println(occAsperFreeBlks);
		if (!occAsperFreeBlks.containsAll(occupiedBlks))
		{
			ArrayList<Integer> remFree = new ArrayList<Integer>(occupiedBlks);
			remFree.removeAll(occAsperFreeBlks);
			freeBlks.removeAll(remFree);
			isChanged = true;
			System.out.println("Notification 3 : " + remFree.toString() + " Blocks removed from free block list");
			
		}
		
		if (!occupiedBlks.containsAll(occAsperFreeBlks))
		{
			ArrayList<Integer> addFree = new ArrayList<Integer>(occAsperFreeBlks);
			addFree.removeAll(occupiedBlks);
			freeBlks.addAll(addFree);
			isChanged = true;
			System.out.println("Notification 3 : " + addFree.toString() + " Blocks added to free block list");
		}
		
//		System.out.println(availableBlks);
//		System.out.println(occupiedBlks);
//		System.out.println(occAsperFreeBlks);
//		System.out.println(freeBlks);
//		System.out.println(availableBlks.size());
//		System.out.println(occupiedBlks.size());
//		System.out.println(freeBlks.size());
		
		if(isChanged)
			writeFreeBlocks(freeBlks, sb);
		System.out.println("Notification 3 : Free block list has been validated ");
	}
	

	private static void writeFreeBlocks(ArrayList<Integer> freeBlks, SuperBlock sb)
	{
		int st = sb.freeStart;
		freeBlks.sort(null);
		StringBuilder s = new StringBuilder();
		
		for (int fb: freeBlks)
		{
			if (st <= sb.freeEnd)
			{
				if (fb%400 != 0)
				{
					s.append(fb+", ");
				}
				else
				{
					writeFile(s.substring(0, s.length()-2), st);
					st++;
					s = new StringBuilder();
					s.append(fb+", ");
				}
			}
		}
		writeFile(s.substring(0, s.length()-2), st);
	}
	
}

class SuperBlock
{
	int devId, mounted, freeStart, freeEnd, root, maxBlocks;
	long creationTime;
	
	public SuperBlock()
	{
		
	}
	
	public SuperBlock(String creationTime, String mounted, String devId, String freeStart, String freeEnd, String root, String maxBlocks)
	{
		this.creationTime = Long.parseLong(creationTime);
		this.mounted = Integer.parseInt(mounted);
		this.devId = Integer.parseInt(devId);
		this.freeStart = Integer.parseInt(freeStart);
		this.freeEnd = Integer.parseInt(freeEnd);
		this.root = Integer.parseInt(root);
		this.maxBlocks = Integer.parseInt(maxBlocks);
	}
	
	public String toString()
	{
		StringBuilder sbString = new StringBuilder();
		sbString.append("{");
		sbString.append("creationTime: " + this.creationTime);
		sbString.append(", mounted: " + this.mounted);
		sbString.append(", devId:" + this.devId);
		sbString.append(", freeStart:" + this.freeStart);
		sbString.append(", freeEnd:" + this.freeEnd);
		sbString.append(", root:" + this.root);
		sbString.append(", maxBlocks:" + this.maxBlocks);
		sbString.append("}");
		
				
		return sbString.toString();
	}		
			
}


class Directory
{
	int size, uid, gid, mode, linkcount;
	long atime, ctime, mtime;
	String filename_to_inode_dict;
	
	public Directory()
	{
		
	}
	
	public Directory(String size, String uid, String gid, String mode, String atime, String ctime, String mtime, 
			String linkcount, String filename_to_inode_dict)
	{
		this.size = Integer.parseInt(size);
		this.uid = Integer.parseInt(uid);
		this.gid = Integer.parseInt(gid);
		this.mode = Integer.parseInt(mode);
		this.atime = Long.parseLong(atime);
		this.ctime = Long.parseLong(ctime);
		this.mtime = Long.parseLong(mtime);
		this.linkcount = Integer.parseInt(linkcount);
		this.filename_to_inode_dict = filename_to_inode_dict;
		
	}
	
	public String toString()
	{
		StringBuilder sbString = new StringBuilder();
		sbString.append("{");
		sbString.append("size:" + this.size);
		sbString.append(", uid: " + this.uid);
		sbString.append(", gid:" + this.gid);
		sbString.append(", mode:" + this.mode);
		sbString.append(", atime:" + this.atime);
		sbString.append(", ctime:" + this.ctime);
		sbString.append(", mtime:" + this.mtime);
		sbString.append(", linkcount:" + this.linkcount);
		sbString.append(", filename_to_inode_dict: " + this.filename_to_inode_dict);
		sbString.append("}");
		
				
		return sbString.toString();
	}		
			
}


class File
{
	int size, uid, gid, mode, linkcount, indirect, location;
	long atime, ctime, mtime;
			
	public File()
	{
		
	}
	
	public File(String size, String uid, String gid, String mode, String linkcount, String atime, String ctime, String mtime, 
			 String indirect, String location)
	{
		this.size = Integer.parseInt(size);
		this.uid = Integer.parseInt(uid);
		this.gid = Integer.parseInt(gid);
		this.mode = Integer.parseInt(mode);
		this.linkcount = Integer.parseInt(linkcount);
		this.atime = Long.parseLong(atime);
		this.ctime = Long.parseLong(ctime);
		this.mtime = Long.parseLong(mtime);
		this.indirect = Integer.parseInt(indirect);
		this.location = Integer.parseInt(location);
		
	}
	
	public String toString()
	{
		StringBuilder sbString = new StringBuilder();
		sbString.append("{");
		sbString.append("size:" + this.size);
		sbString.append(", uid: " + this.uid);
		sbString.append(", gid:" + this.gid);
		sbString.append(", mode:" + this.mode);
		sbString.append(", linkcount:" + this.linkcount);
		sbString.append(", atime:" + this.atime);
		sbString.append(", ctime:" + this.ctime);
		sbString.append(", mtime:" + this.mtime);
		sbString.append(", indirect:" + this.indirect);
		sbString.append(" location:" + this.location);
		sbString.append("}");
		
				
		return sbString.toString();
	}		
			
}

class DirValidation
{
	boolean isLnkcM, isatM, isctM, ismtM, isDtP, isDtDtP;

}

class FileValidation
{
	boolean isIndirct, isatM, isctM, ismtM, isLoc;

}

