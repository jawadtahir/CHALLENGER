package de.tum.i13.datasets.financial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class SymbolsGenerator implements Enumeration<List<String>>{

    private ArrayList<SymbolStat> symbols;
    private Random gen;
    private int treshold;

    public SymbolsGenerator(ArrayList<SymbolStat> symbols) {
        this.symbols = symbols;
        this.treshold = this.symbols.size() /10;
        this.gen = new Random(123);
    }

    @Override
    public boolean hasMoreElements() {
        return true;
    }

    @Override
    public List<String> nextElement() {

        ArrayList<String> takenSymbols = new ArrayList<String>();

        int amount_symbols = gen.nextInt(15) + 5; //between 5 - 20 symbols

        var amount_lower = Math.max(amount_symbols / 10, 1); //10% we get from the lower 90%
        var amount_upper = amount_symbols - amount_lower;

        for(int i = 0; i < amount_lower; ++i) {
            takenSymbols.add(this.symbols.get(this.gen.nextInt(treshold)).getSymbol());
        }

        for(int i = 0; i < amount_upper; ++i) {
            takenSymbols.add(this.symbols.get(this.gen.nextInt((this.symbols.size() -1))+treshold).getSymbol());
        }

        var distinct_symbols = takenSymbols.stream().distinct().collect(Collectors.toList());
        Collections.shuffle(symbols, this.gen);

        return distinct_symbols;
    }
    
}
