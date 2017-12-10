package ch.fhnw.jobannotations.backup; /**
 ** Java Program to implement ch.fhnw.jobannotations.backup.Bitap Algorithm
 **/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/** Class ch.fhnw.jobannotations.backup.Bitap **/
public class Bitap
{
    /** function findPattern **/
    public void findPattern(String t, String p)
    {
        char[] text = t.toCharArray();
        char[] pattern = p.toCharArray();
        int pos = bitap_search(text, pattern);
        if (pos == -1)
            System.out.println("\nNo Match\n");
        else
            System.out.println("\nPattern found at position : "+ pos);
    }
    /** ch.fhnw.jobannotations.backup.Bitap search **/
    private int bitap_search(char[] text, char[] pattern)
    {
        int m = pattern.length;
        long pattern_mask[] = new long[Character.MAX_VALUE + 1];
        /** Initialize the bit array R **/
        long R = ~1;
        if (m == 0)
            return -1;
        if (m > 63)
        {
            System.out.println("Pattern is too long!");
            return -1;
        }
        /** Initialize the pattern bitmasks **/
        for (int i = 0; i <= Character.MAX_VALUE; ++i)
            pattern_mask[i] = ~0;
        for (int i = 0; i < m; ++i)
            pattern_mask[pattern[i]] &= ~(1L << i);
        for (int i = 0; i < text.length; ++i)
        {
            /** Update the bit array **/
            R |= pattern_mask[text[i]];
            R <<= 1;
            if ((R & (1L << m)) == 0)
                return i - m + 1;
        }
        return -1;
    }
    /** ch.fhnw.jobannotations.backup.Main Function **/
    public static void main(String[] args) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("ch.fhnw.jobannotations.backup.Bitap Algorithm Test\n");
        System.out.println("\nEnter Text\n");
        String text = br.readLine();
        System.out.println("\nEnter Pattern\n");
        String pattern = br.readLine();
        Bitap b = new Bitap();
        b.findPattern(text, pattern);
    }
}
