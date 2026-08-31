package tvs.mcsb;

public class Utility {
    // Used for code snipits that would be reused across the project

    public static boolean logCheck(int logValueMin) {
        if(ConfigHelper.logLevel >= logValueMin && (ConfigHelper.debugEnabled == true || logValueMin == 1)){
            return true;
        } else {
            return false;
        }
    }
}
