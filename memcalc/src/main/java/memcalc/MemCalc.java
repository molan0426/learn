package memcalc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class MemCalc {
	private static final int[] DAYS = {1,3,7,14};
	private static final SimpleDateFormat csvFmt = new SimpleDateFormat("H:mm");
	private static final SimpleDateFormat confFmt = new SimpleDateFormat("yyyy-MM-dd H:mm");
	
	private static String CONF_FILE = "./memstarts.conf";
	private static String CSV_FILE = "./schedule.csv";
	IMemStarts memStarts;
	public MemCalc(IMemStarts memStarts) {
		this.memStarts = memStarts;
	}
	public String[] calcThisWeek(){
		Calendar calendar=Calendar.getInstance();
		return calcTHeWeek(calendar.getTime());
	}
	
	public String[] calcNextWeek(){
		Calendar calendar=Calendar.getInstance();
		calendar.add(Calendar.WEEK_OF_MONTH, 1);
		calendar.set(Calendar.DAY_OF_WEEK, 1);
		return calcTHeWeek(calendar.getTime());
	}
	
	private String[] calcTHeWeek(Date date){
		Calendar calendar=Calendar.getInstance();
		calendar.setTime(date);
		String[] ret = new String[7];
		int day = calendar.get(Calendar.DAY_OF_WEEK);
		for( int i = 1;i< day;i++) {
			ret[i-1] =  "";
		}
		for(int i = day ;i<=7;i++) {
			calendar.setTime(date);
			calendar.add(Calendar.DAY_OF_WEEK, i - day);
			ret [i - 1] = calcTheDay(calendar);
		}

		return ret;
	}
	
	private String calcTheDay(Calendar calendar) {
		String ret = "";
		int year = calendar.get(Calendar.YEAR);
		int day = calendar.get(Calendar.DAY_OF_YEAR);
		
		Map<String, Date> starts = memStarts.getStarts();
		for (Map.Entry<String, Date> entry : starts.entrySet()) {
		
			Date startDate = entry.getValue();
			String cur = "";
			Date[] days = getCalendersOf(startDate);
			for(int i = 0;i<days.length;i++) {
				calendar.setTime(days[i]);
				if(calendar.get(Calendar.YEAR) == year) {
					int theDay = calendar.get(Calendar.DAY_OF_YEAR);
					if( theDay == day) {
						//the right day
						if(cur.length() != 0)
							cur += ",";
						cur += csvFmt.format(days[i]);
					}else if(theDay < day) {
						continue;
					}else {
						break;
					}
				}
			}
			if(cur.length() != 0) {
				if(ret.length() != 0) {
					ret += "\n";
				}
				ret += entry.getKey() +" " + cur;
			}
			
		}
		return ret;
	}
	
	private Date[] getCalendersOf(Date start) {//15days one circle
		int index = 0;
		Date[] ret = new Date[7];
		Calendar calendar=Calendar.getInstance();
		calendar.setTime(start);
		
		calendar.add(Calendar.MINUTE,5);
		ret[index++] = calendar.getTime();
		calendar.setTime(start);
		calendar.add(Calendar.MINUTE,30);
		ret[index++] = calendar.getTime();
		calendar.setTime(start);
		calendar.add(Calendar.HOUR_OF_DAY,12);
		ret[index++] = calendar.getTime();
		
		for(int i = 0;i<DAYS.length;i++) {
			calendar.setTime(start);
			calendar.add(Calendar.DAY_OF_MONTH,DAYS[i]);
			ret[index++] = calendar.getTime();
//			System.out.println(format.format(calendar.getTime()));
		}
		return ret;
	}
	
	private void printOut(String[] schedule, PrintStream out) {
		for(int i = 0;i<schedule.length;i++) {
			String str = schedule[i];
			if(str.length() != 0) {
				out.print("\"" +str + "\"");
			}
			if(i != schedule.length -1)
				out.print(",");
		}
		out.println();		
	}
	private static void initTestData(HashMap<String, Date> map) {
		Calendar calend = Calendar.getInstance();
		calend.set(Calendar.HOUR_OF_DAY, 8);
		map.put("Test1", calend.getTime());
		calend.add(Calendar.DAY_OF_MONTH, 1);
		map.put("Test2", calend.getTime());
	}
	private static void initFromFile(HashMap<String, Date> map) throws IOException, ParseException {
		FileReader reader = new FileReader(CONF_FILE);
		BufferedReader in = new BufferedReader(reader);
		String line;
		while((line = in.readLine()) != null) {
			line = line.trim();
			if(line.length() == 0)
				continue;
			int idx = line.indexOf('=');
			if(idx != -1) {
				String content = line.substring(0, idx);
				String time = line.substring(idx+1);
//				System.out.println(time);
				map.put(content, confFmt.parse(time));
			}
		}
		reader.close();
	}
	private static String[] parseCmd(String sCmd) {
		String[] ret = null;
		int idx = sCmd.indexOf('=');
		if(idx != -1) {
			String cmd = sCmd.substring(0, idx);
			String value = sCmd.substring(idx+1);
			ret = new String[]{cmd,value};
		}
		return ret;
	}
	public static void main(String[] args) {
		if(args.length == 1) {
			String arg = args[0];
			if("-h".equalsIgnoreCase(arg) ||
					"help".equalsIgnoreCase(arg)||
					"--help".equalsIgnoreCase(arg)) {
				System.out.println("java -jar memcalc.jar <new=name of new list> <date=start date of new list> <conf=config file> <csv=csv file>");
				return;
			}
		}
		final HashMap<String, Date> map = new HashMap<>();
//		initTestData(map);
		try {
			String newContent = null;
			Date newDate = null;
			for(int i = 0;i<args.length;i++) {
				String[] cmd = parseCmd(args[i]);
				if("new".equalsIgnoreCase(cmd[0])) {
					//new a list
					newContent = cmd[1];
				}else if("date".equalsIgnoreCase(cmd[0])) {
					newDate = confFmt.parse(cmd[1]);
				}else if("conf".equalsIgnoreCase(cmd[0])) {
					CONF_FILE = cmd[1];
				}else if("csv".equalsIgnoreCase(cmd[0])) {
					CSV_FILE=cmd[1];
				}
			}
			if(newContent != null) {
				FileOutputStream fout = new FileOutputStream(CONF_FILE,true);
				PrintStream pout = new PrintStream(fout);
				if(newDate == null)
					newDate = new Date();
				pout.println(newContent+"="+confFmt.format(newDate));
				fout.close();
			}
			initFromFile(map);
//			System.out.println(map);
			
			IMemStarts starts = new IMemStarts() {
				@Override
				public Map<String, Date> getStarts() {
					// TODO Auto-generated method stub
					return map;
				}
			};

			MemCalc memCalc = new MemCalc(starts);
			FileOutputStream fout = new FileOutputStream(CSV_FILE);
			PrintStream pout = new PrintStream(fout);
			
			memCalc.printOut(memCalc.calcThisWeek(), pout);
			memCalc.printOut(memCalc.calcNextWeek(), pout);
			pout.close();
			fout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
