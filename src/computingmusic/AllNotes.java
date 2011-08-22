/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package computingmusic;


import java.awt.Point;
import java.util.ArrayList;

import computingmusic.utils.StringFingeringConstants;

/**
 *
 * @author eliot
 */
public class AllNotes {

    public static int[] ofInstr(Instrument v, int pc) {
        ArrayList<Integer> ans = new ArrayList<Integer>();
        //int[] range = Fing.getRange(v);
        //int[] range = v.getRange();
        Point range = v.getRange();
        for (int i = pc; i < StringFingeringConstants.MIDI_HIGH_NOTE-1; i = i + 12) 
        {
        	if //(i >= range[0] && i <= range[1]) 
        		(i >= range.x && i <= range.y) 
        	{
                ans.add(i);
            }	
        }
        return Lis.ArrayofList(ans);
    }

    public static String keyName(String base, int pitch) {
        return base + (pitch / 12);

    }

    public static ArrayList<Keynum.K> ksofPc(Instrument v, Keynum.K pcK) {
        int pc = pcK.pitch;
        String pcName = pcK.name;
        String chroma = pcK.chromaName;
        int[] pitches_in_range = ofInstr(v, pc);
        // in order to print correctly, we need
        // to know how many octaves to add to name.
        ArrayList<Keynum.K> ks = new ArrayList<Keynum.K>();
        for (int i = 0; i < pitches_in_range.length; i++) {
            int pitch = pitches_in_range[i];
            int octs = pitch / 12;
            Keynum.K k = new Keynum.K();
            k.accidentals = pcK.accidentals;
            k.chromaName = chroma;
            k.pitch = pitch;
            k.pc = pc;
            k.name = pcName + octs;
            k.octaves = octs;
            ks.add(k);
        }
        return ks;
    }


    // given a k, if it's a PC then make list of all pcs in range,
    //                         else return singleton.
    public static ArrayList<Keynum.K> enlist(Instrument v, Keynum.K k) {
        if (k.isPc) {
            return ksofPc(v, k);
        } else {
            ArrayList<Keynum.K> ks = new ArrayList<Keynum.K>();
            ks.add(k);
            return ks;
        }
    }

    // given list of ks, some of which are PCs, enlist and make a chinese menu.
    public static ArrayList<ArrayList<Keynum.K>> chineseKs(Instrument v,ArrayList<Keynum.K> ks){
        // I feel this is too verbose a syntax for let k2 = ref [];;
        ArrayList<ArrayList<Keynum.K>> k2 = new ArrayList<ArrayList<Keynum.K>>();

        for (Keynum.K k : ks){
            k2.add(enlist(v, k));
        }
        return Lis.chineseMenu(k2);
    }


    public static void main(String args[]) {
        ArrayList<Keynum.K> ks = Keynum.parseKeyStringList("c# d d#");
        ArrayList<ArrayList<Keynum.K>> cks = chineseKs(new Instrument (Instrument.Instr.VC), ks);
        System.out.println("Here are some notes");
        for (ArrayList<Keynum.K> k1  : cks) {
            for (Keynum.K xx : k1)
                System.out.print( "("+xx.tostring() + ")  ");
            System.out.println("");
        }
        // System.out.println("TEST: " + keyName("d#", 63));

    }
}
