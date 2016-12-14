/*************************************************************************
 * tranSMART Data Loader - ETL tool for tranSMART
 * 
 * Copyright 2012-2013 Thomson Reuters
 * 
 * This product includes software developed at Thomson Reuters
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software  
 * Foundation, either version 3 of the License, or (at your option) any later version, along with the following terms:
 * 1.	You may convey a work based on this program in accordance with section 5, provided that you retain the above notices.
 * 2.	You may convey verbatim copies of this program code as you receive it, in any medium, provided that you retain the above notices.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS    * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *
 ******************************************************************/

package com.thomsonreuters.lsps.transmart.etl

import java.sql.SQLException

enum LogType { MESSAGE, WARNING, ERROR, DEBUG, PROGRESS }

class Logger {
    private static boolean interactiveMode

    public static void setInteractiveMode(boolean interactiveMode) {
        this.interactiveMode = interactiveMode
    }

    public static boolean isInteractiveMode() {
        return interactiveMode
    }

    public static Logger getLogger(Class<?> cls) {
        return new Logger()
    }
	
	private String timestamp(LogType ltype) {
		def tm = new Date()
		def str = String.format('%tm/%<td %<tT ', tm)
		
		switch (ltype) {
			case LogType.DEBUG:
				str += 'DBG '
				break
			case LogType.WARNING: 
				str += 'WAR '
				break
			case LogType.ERROR: 
				str += 'ERR '
				break
			case LogType.MESSAGE: 
			default:
				str += 'MSG '
		}
		
		return str
	}

    void log(LogType ltype, Exception ex) {
        log(ltype, ex.message, ex)
    }

	void log(LogType logType, String message, Exception ex) {
		StringWriter stringWriter = new StringWriter()
		PrintWriter writer = new PrintWriter(stringWriter)
		if (message) {
			writer.append("Message: ").println(message)
		}
		writer.append('Exception: ')
		ex.printStackTrace(writer)
		if (ex instanceof SQLException && ex.nextException != null) {
			writer.append('Next exception: ')
			ex.nextException.printStackTrace(writer)
		}
		writer.flush()
		log(logType, stringWriter.toString())
	}

	void logAndThrow(String message, Exception ex) {
		log(LogType.ERROR, message, ex)
	}

	void logAndThrow(Exception ex) {
		log(LogType.ERROR, ex)
		throw ex
	}

	void log(LogType ltype, str) {
		if (ltype != LogType.PROGRESS) {
			str = timestamp(ltype) + str
			if (ltype == LogType.ERROR)
				System.err.println(str)
			else
				println str
		}
		else if (Logger.isInteractiveMode()) {
            print '\r' + str
        }
	}
	
	void log(str) {
		log(LogType.MESSAGE, str)
	}

}
