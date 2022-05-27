package de.tum.i13.datasets.financial;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import de.tum.i13.bandency.Event;
import de.tum.i13.bandency.SecurityType;
import de.tum.i13.datasets.airquality.StringZipFileIterator;
import de.tum.i13.datasets.cache.CloseableSource;

public class FinancialEventLoader implements CloseableSource<Event> {

    private StringZipFileIterator csvFile;
    private Event curr;
    private boolean firstCall;
    private long errcnt;
    private long skipedcnt;
    private long cnt;

    private DateTimeFormatter dateTimeParser = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS"); //00:00:00.000
    
    public FinancialEventLoader(StringZipFileIterator csvFile) {
        this.csvFile = csvFile;
        this.curr = null;
        this.firstCall = true;
        this.errcnt = 0;
        this.skipedcnt = 0;
        this.cnt = 0;
    }

    private boolean isHeaderCorrect(String firstLine) {
        String controlHeader = "ID,SecType,Date,Time,Ask,Ask volume,Bid,Bid volume,Ask time,Day's high ask,Close,Currency,Day's high ask time,Day's high,ISIN,Auction price,Day's low ask,Day's low,Day's low ask time,Open,Nominal value,Last,Last volume,Trading time,Total volume,Mid price,Trading date,Profit,Current price,Related indices";

        String[] splitted = firstLine.split(",");
        if(splitted.length == 30) {
            return firstLine.equalsIgnoreCase(controlHeader);
        }

        return false;
    }

    private void setupNewFile() throws IOException {
        if(this.csvFile != null) {
            String firstLine = this.csvFile.nextElement();
            if(!isHeaderCorrect(firstLine)) {
                throw new IOException("invalid data");
            }
            this.csvFile.nextElement(); // skip descriptions
            this.csvFile.nextElement(); // skip descriptions
        }
    }

    private Event parseFromString(String nextLine) {
        if (nextLine == null) {
            return null;
        }

        String[] splitted = nextLine.split(",", -1);

        SecurityType secType;
        if(splitted[1].equalsIgnoreCase("I")) {
            secType = SecurityType.Index;
        } else if (splitted[1].equalsIgnoreCase("E")) {
            secType = SecurityType.Equity;
        } else {
            return null;
        }

        String symbol = splitted[0];
        String last = splitted[21];
        if(last.equalsIgnoreCase("0")) {
            return null;
        }

        float parsedLast = Float.parseFloat(last);
        if(parsedLast == 0.0) {
            return null;
        }

        String date = splitted[26]; //08-11-2021
        if (date.isEmpty()) {
            date = splitted[2];
        }
        String time = splitted[23]; //00:00:00.000
        if (time.isEmpty()) {
            time = splitted[3];
        }
        String dateTimeString = date + " " + time;

        if(symbol.isEmpty() || last.isEmpty() || date.isEmpty() || time.isEmpty()) {
            return null;
        }

        var dateTime = LocalDateTime.parse(dateTimeString, dateTimeParser);
        com.google.protobuf.Timestamp ts = com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(dateTime.toEpochSecond(ZoneOffset.UTC))
                .setNanos(dateTime.getNano())
                .build();
        
        
        Event ev = Event.newBuilder()
            .setSymbol(symbol)
            .setSecurityType(secType)
            .setLastTrade(ts)
            .setLastTradePrice(parsedLast)
            .build();

        return ev;
    }

    private void parseNext() {
        while(curr == null && this.csvFile.hasMoreElements()) {
            String nextElement = "";
            try {
                nextElement = this.csvFile.nextElement();
                
                Event event = parseFromString(nextElement);

                if(event == null) {
                    //skipped
                    ++this.skipedcnt;
                    continue;
                }

                curr = event;
            } catch(Exception ex) {
                ++errcnt;
            }
        }
    }

    @Override
    public boolean hasMoreElements() {
        if(this.firstCall) {
            try {
                setupNewFile();
                parseNext();
                this.firstCall = false;
            } catch(IOException e) {

            }
        }

        return curr != null;
    }

    @Override
    public Event nextElement() {
        if(this.firstCall) {
            try {
                setupNewFile();
                parseNext();
                this.firstCall = false;
            } catch(IOException e) {

            }
        }

        Event forReturn = curr;
        curr = null;
        parseNext();
        ++cnt;

        return forReturn;
    }

    @Override
    public void close() throws IOException {
        this.csvFile.close();        
    }
}
