package de.tum.i13.datasets.financial;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

public class SymbolsReader {
    private String filename;

    public SymbolsReader(String filename) {
        this.filename = filename;
    }

    public ArrayList<SymbolStat> readAll() throws IOException {
        var symbols = new ArrayList<SymbolStat>();

        InputStream inputstream = new FileInputStream(this.filename);
        Reader r = new InputStreamReader(inputstream);
        try (BufferedReader br = new BufferedReader(r)) {
            String line = br.readLine();
            while(line != null) {

                String[] parts = line.trim().split(" ");
                var cnt = Integer.parseInt(parts[0]);
                var symbol = parts[1];

                var ss = new SymbolStat(cnt, symbol);
                symbols.add(ss);


                line = br.readLine();
            }
        }
        
        return symbols;
    }
}
