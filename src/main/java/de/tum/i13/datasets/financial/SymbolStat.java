package de.tum.i13.datasets.financial;

public class SymbolStat {

    private int occurances;
    private String symbol;

    public SymbolStat(int occurances, String symbol) {
        this.occurances = occurances;
        this.symbol = symbol;
    }

    public int getOccurances() {
        return occurances;
    }

    public String getSymbol() {
        return this.symbol;
    }    
}
