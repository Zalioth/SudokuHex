/**
 * @file Grid.java
 * @brief SudokuHex Grid class
 *
 * @version 1.1
 * @author José Ignacio Carmona Villegas <joseicv@correo.ugr.es>
 * @date 25/October/2012
 *
 * @thanks Fernando Berzal Galiano <berzal@acm.org>
 * @thanks Carlos Cano Gutiérrez <ccano@decsai.ugr.es>
 *
 * @section LICENSE
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details at
 * http://www.gnu.org/copyleft/gpl.html
 * 
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * @brief Hexadecimal Sudoku.
 * Represents and solves a hexadecimal sudoku.
 */
public class SudokuHex 
{	
	private enum Rows
	{
		A("A",0), B("B",0), C("C",0), D("D",0), E("E",1), F("F",1), G("G",1), H("H",1),
		I("I",2), J("J",2), K("K",2), L("L",2), M("M",3), N("N",3), O("O",3), P("P",3);
		
		private final String s;
		private final int superRow;
		
		Rows(String s, int superRow)
		{
			this.s = s;
			this.superRow = superRow;
		}
		
		public String toString()
		{
			return s;
		}
		
		public int superRow()
		{
			return superRow;
		}
	}
	
	private enum Cols
	{
		a("0",0), b("1",0), c("2",0), d("3",0), e("4",1), f("5",1), g("6",1), h("7",1), 
		i("8",2), j("9",2), k("A",2), l("B",2), m("C",3), n("D",3), o("E",3), p("F",3);
		
		private final String s;
		private final int superColumn;
		
		Cols(String s, int superColumn)
		{
			this.s = s;
			this.superColumn = superColumn;
		}
		
		public String toString()
		{
			return s;
		}
		
		public int superColumn()
		{
			return superColumn;
		}
	}
	
	private String template; /**< Text template that represents a hexadecimal sudoku grid (0-F for values set values, and . or - for free squares. It can also contain carriage returns, tabulations or whitespaces to increase readability). */
	// STATIC DICTIONARIES
	private TreeSet<String> squares; /**< Set of names of the square that compose the hexadecimal sudoku grid. Basic operations have logarithmic order, and it maintains the lexicographic order of its elements. */
	private HashMap<String, ArrayList<TreeSet<String>>> units; /**< */
	private HashMap<String, TreeSet<String>> peers; /**< */
	// DYNAMIC DICTIONARY
	private HashMap<String, String> values; /**< */
	private int numberBacktracks;
	private int timesUsedHeuristic;
	
	/**
	 * Constructor. Prepares the data structures to be used to solve the hexadecimal sudoku.
	 * @param template String that represents a hexadecimal sudoku.
	 * @pre The template supplied must be a valid sudoku (it must not have two equal numbers in the same row, column and box).
	 * @post The SudokuHex object will be created and ready to be solved.
	 */
	public SudokuHex (String template)
	{
		numberBacktracks = 0;
		timesUsedHeuristic = 0;
		prepare_template(template);
		initialize_squares();
		initialize_units_and_peers();
		initialize_values();
	}
	
	/**
	 * @brief Prepares the template to have its values read.
	 * @param template String that represents a hexadecimal sudoku.
	 * @pre The template supplied must be a valid sudoku (it must not have two equal numbers in the same row, column and box).
	 * @post The characters of the string will be uppercase.
	 * @post The template will not have any whitespaces.
	 * @post The template will not have any tabulations.
	 * @post The template will not have any carriage returns.
	 * @post The template will replace the ambiguous free square symbols ("." or "-") with a single symbol (".").
	 */
	private void prepare_template(String template)
	{
		template = template.toUpperCase();
		template = template.replace(" ", "");
		template = template.replace("\t", "");
		template = template.replace("\n", "");
		template = template.replace("-", ".");
		this.template = template;
	}

	/**
	 * @brief Initializes the values of the dictionary that will keep the squares of the hexadecimal sudoku.
	 * It will be used to define the rows and the boxes. Due to the fact that it's ordered, it can be used as a sudoku iterator.
	 */
	private void initialize_squares()
	{
		squares = new TreeSet<String>();
		
		for (Rows r : Rows.values())
		{
			for (Cols c : Cols.values())
			{
				String square;

				square = r.toString() + c.toString();
				squares.add(square);
			}
		}
	}
	
	/**
	 * @brief Initializes the dictionaries that will represent the units and peers of a square.
	 */
	private void initialize_units_and_peers()
	{
		units = new HashMap<String, ArrayList<TreeSet<String>>>(200);
		peers = new HashMap<String, TreeSet<String>>(200);
		
		// Calculating boxes
		ArrayList<TreeSet<String>> boxlist;
		boxlist = new ArrayList<TreeSet<String>>();

		for (int box = 0; box < 16; ++box)
		{
			String Rows = "ABCDEFGHIJKLMNOP";
			String Columns = "0123456789ABCDEF";
			String startSquare;
			String endSquare;
			TreeSet<String> temp;

			temp = new TreeSet<String>();

			int superRow = (int) box / 4;
			int superColumn = box % 4;
			int row = superRow * 4;
			int column = 4 * superColumn;

			for (int j = row; j < row + 4; ++j)
			{
				startSquare = "" + Rows.charAt(j) + Columns.charAt(column);
				endSquare = "" + Rows.charAt(j) + Columns.charAt(column + 3);
				temp.addAll(squares.subSet(startSquare, true, endSquare, true));
			}
			boxlist.add(temp);
			column += 4;
		}

		// Initializing units and peers
		for (Rows r : Rows.values())
		{
			for (Cols c : Cols.values())
			{
				String square;
				String startSquare;
				String endSquare;
				ArrayList<TreeSet<String>> unitlist;
				TreeSet<String> temp;

				square = r.toString() + c.toString();

				unitlist = new ArrayList<TreeSet<String>>();

				// Adding the row to the unit list
				startSquare = r.toString() + Cols.a.toString();
				endSquare = r.toString() + Cols.p.toString();
				unitlist.add(new TreeSet<String>(squares.subSet(startSquare, true, endSquare, true)));

				// Adding the column to the unit list
				temp = new TreeSet<String>();
				for (Rows i : Rows.values())
				{
					temp.add(i.toString() + c.toString());
				}
				unitlist.add(temp);

				// Adding the box to the unit list
				unitlist.add(boxlist.get((r.superRow() * 4) + c.superColumn()));

				units.put(square, unitlist);

				// Creating the peer list
				temp = new TreeSet<String>();

				temp.addAll(unitlist.get(0));
				temp.addAll(unitlist.get(1));
				temp.addAll(unitlist.get(2));

				// temp is a set, and doesn't allow duplicates, so there's
				// no need to remove the duplicates from the unit list.
				// The only element left to remove is the square that refers to.
				temp.remove(square);
				
				// Now that the peers are all in a single set, assign it to the corresponding square.
				peers.put(square, temp);
			}
		}
	}
	

	/**
	 * @brief Initializes the dictionary of values corresponding to a square.
	 * Each square has a set of possible values. If the correct value is known, then it will be only one.
	 * If the correct value isn't known, then the set will have all the possible values.
	 * @post Each assigned square will have only its assigned value.
	 * @post Each unassigned square from the template will have all possible values.
	 */
	private void initialize_values()
	{
		values = new HashMap<String, String>(200);
		int index = 0;
		
		for (Rows r : Rows.values())
		{
			for (Cols c : Cols.values())
			{
				// The value from the template will be read, and it will be added to the values set.
				// If the value read is the empty symbol (. or -) then all the possible values will be added to the set.
				if(template.charAt(index) == '.')
				{
					// Free square, add all the possible values
					String all_possible_values = "";
					for(Cols digits : Cols.values())
					{
						all_possible_values += digits.toString();
					}
					values.put(r.toString()+c.toString(), all_possible_values);
				}
				else
				{
					// Established square, add only the read value
					String single_value = "" + template.charAt(index);
					values.put(r.toString()+c.toString(), single_value);
				}
				
				++index;
			}
		}
		
		// Now that the values are set, the data structure must be taken to a consistent state, so 
		// later, when propagating the restrictions and backtracking, the decisions made are also consistent.
		for (Rows r : Rows.values())
		{
			for (Cols c : Cols.values())
			{
				// For each square, if it has only one possible value (if it is set), remove the value from its peers.
				if(values.get(r.toString()+c.toString()).length() == 1)
				{
					for(String peer : peers.get(r.toString()+c.toString()))
					{
						values.put(peer, values.get(peer).replace(values.get(r.toString()+c.toString()), ""));
					}
				}
			}
		}
	}
	
	/**
	 * @brief Solves the sudoku and changes the values content to a solved state.
	 * @pre
	 * @post
	 */
	public void solve()
	{
		this.values = search(this.values);
		// If the values object returned is a null object, there's no solution for the sudoku
	}
	
	/**
	 * @brief
	 * @param
	 * @return
	 */
	private HashMap<String, String> search(HashMap<String, String> values)
	{
		if(values == null)
		{
			// Already failed...
			return null;
		}
		// Check goal condition (if the sudoku is solved) 
		// and keep track of the node that minimizes the branching factor (the square with minimal value choices (different from zero)).
		boolean solved = true;
		String bestSquare = Rows.A.toString()+Cols.a.toString();
		
		for (Rows r : Rows.values())
		{
			for (Cols c : Cols.values())
			{
				String testSquare = r.toString() + c.toString();
				
				if(values.get(testSquare).length()==0)
				{
					// Contradiction! The test square is invalid!
					return null;
				}
				else
				{
					// Update the goal condition flag.
					solved &= (values.get(testSquare).length() == 1);
					
					// Update the best square.
					if(values.get(bestSquare).length() == 1)
					{
						// The best square is useless (only one possible value, no decision to make)
						// Pick the first one that doesn't have only one value.
						if(values.get(testSquare).length() > 1)
						{
							bestSquare = testSquare;
						}
					}
					else if(values.get(testSquare).length() < values.get(bestSquare).length() && values.get(testSquare).length() > 1)
					{
						// Assign the test square if it's better (already tested that it's valid).
						bestSquare = testSquare;
					}
				}
			}
		}
		
		if(solved)
		{
			// Solved!
			return values;
		}
		else
		{
			// Not solved, pick the tree with least branches from our forest of solution trees.
			// The possible values for the square are chosen as they come. No heuristic is applied.
			// For each possible value of the best square...
			HashMap<String, String> originalValues = new HashMap<String, String>(values); // The Strings are immutable objects in Java, there's no need to deep copy them.
			for(int i = 0; i < originalValues.get(bestSquare).length(); i++)
			{
				// Assign it to the square
				values = assign(values, bestSquare, originalValues.get(bestSquare).charAt(i));
				// Search depth-first
				HashMap<String, String> obtainedValues = search(values);
				// If the value returned is a solution (not a null object), solved!
				if(obtainedValues != null)
				{
					return obtainedValues;
				}
				numberBacktracks++;
				// If the value returned is null, the sub-branch doesn't have a solution, try the next value!
				// Before trying another value, the original values must be restored.
				values = new HashMap<String, String>(originalValues);
			}
			// If after checking all the values, none provides a solution, this branch doesn't have a solution, backtrack!
			return null;
		}
	}
	
	/**
	 * @brief
	 * @param values
	 * @param square
	 * @param value
	 * @return
	 */
	private HashMap<String, String> assign(HashMap<String, String> values, String square, char value)
	{
		// Eliminate all values different to value
		// If they all succeed, return the values data structure
		// otherwise, return null
		String valuesToRemove = values.get(square).replace(""+value, "");
		
		for(char v : valuesToRemove.toCharArray())
		{
			values = eliminate(values, square, v);
			
			if(values == null)
			{
				return null;
			}
		}
		return values;
	}
	
	/**
	 * @brief
	 * @param values
	 * @param square
	 * @param value
	 * @return
	 */
	private HashMap<String, String> eliminate(HashMap<String, String> values, String square, char value)
	{
		if(!values.get(square).contains(""+value))
		{
			// Already eliminated
			return values;
		}
		
		// Eliminate the value
		String temp = values.get(square).replace(""+value, "");
		if(temp.isEmpty())
		{
			// Contradiction, attempting to remove the last value!
			return null;
		}
		values.put(square, temp);
		
		// If now that the value was eliminated, we find that there's only one possible value
		// left for this square (same effect as assigning the value to this square)...
		// propagate the restrictions, eliminating this new found value through the square's peers
		// If any of the eliminations returns null, it means it couldn't be done, and we have to propagate the failure
		// up in the tree and backtrack.
		if(temp.length() == 1)
		{
			for(String s : peers.get(square))
			{
				values = eliminate(values, s, temp.charAt(0));
				if(values == null)
				{
					return null;
				}
			}
		}
		
		// After that, we have to check the units of the square, and get all the squares with the value (the one passed in the function)
		// If there's no square that has that value as a possible one, then the choice of eliminating this value was wrong
		// in the first place! return null.
		// If there's only one place left for it, assign is called and eliminate returns the assignation's results.
		// If there's more than one place left for the value, then there's a choice we may want to backtrack in case it
		// failed, better leave it alone for now.
		
		TreeSet<String> toCheck = new TreeSet<String>();
		for(TreeSet<String> u : units.get(square))
		{
			// For each unit (row, column and box) which the square belongs to...
			for(String s : u)
			{
				if(values.get(s).contains(""+value))
				{
					toCheck.add(s);
				}
			}
		}
		if(toCheck.size() == 0)
		{
			return null;
		}
		else if(toCheck.size() == 1)
		{
			return assign(values, toCheck.first(), value);
		}
		else
		{
			// More than one option, no choice is made here, that's the backtracking search's responsibility.
			
			// Before returning, a final consistency check will be made.
			// Sudoku is declared as an AllDiff constraint. We can check in advance for a valid solution by doing the following simple form of inconsistency detection: if there are M variables involved in the constraint and if they have N possible distinct values altogether, and M>N, then the constraint cannot be satisfied (and we don't have to spend time on this branch that will eventually go wrong). 
			
			for(TreeSet<String> u : units.get(square))
			{
				int m = 0;
				int n = 0;
				String possibleValues = "";
				// For each unit (row, column and box) which the square belongs to...
				for(String s : u)
				{
					++m;
					for(char c : values.get(s).toCharArray())
					{
						if(!possibleValues.contains(""+c))
						{
							++n;
							possibleValues += c;
						}
					}
				}
				if(m > n)
				{
					timesUsedHeuristic++;
					return null;
				}
			}
			
			return values;
		}
	}
	
	/**
    * Represents the hexadecimal sudoku as a string.
	 * @see java.lang.Object#toString()
	 */
	public String toString ()
	{
		String s = "";
		if(values == null)
		{
			return "The sudoku does not have a solution";
		}
		for (Rows r : Rows.values())
		{
			for (Cols c : Cols.values())
			{
				s += values.get(r.toString()+c.toString());
			}
		}
		return s;
	}
	
	/**
	 * @brief Prints in the default system output several tests related to the data structures.
	 * @deprecated Only used for testing purposes.
	 */
	protected void test_data_structures()
	{
		//System.out.println(squares.size());
		//System.out.println(units.size());
		//System.out.println(peers.size());
		
		//System.out.println(squares.last());
		//System.out.println(units.get(squares.last()).size());
		//System.out.println(peers.get(squares.last()).size());
		//System.out.println(units.get(squares.last()));
		//System.out.println(peers.get(squares.last()));
		
		//System.out.println(template);
		
		for (Rows r : Rows.values())
		{
			for (Cols c : Cols.values())
			{
				System.out.print("\t"+r.toString()+c.toString()+"\t");
				System.out.print(values.get(r.toString()+c.toString()));
			}
			System.out.println("\n");
		}
		
		
	}
	
	protected void print_values()
	{
		for (Rows r : Rows.values())
		{
			for (Cols c : Cols.values())
			{
				System.out.print("\t"+r.toString()+c.toString()+"\t");
				System.out.print(values.get(r.toString()+c.toString()));
			}
			System.out.println("\n");
		}
	}
	
	protected void print_values(HashMap<String, String> values)
	{
		if(values == null)
		{
			System.out.println("null values returned");
			return;
		}
		for (Rows r : Rows.values())
		{
			for (Cols c : Cols.values())
			{
				System.out.print("\t"+r.toString()+c.toString()+"\t");
				System.out.print(values.get(r.toString()+c.toString()));
			}
			System.out.println("\n");
		}
	}
	
	protected void show_values(HashMap<String, String> values, String square)
	{
		if(values == null)
		{
			System.out.println("null values returned");
			return;
		}
		System.out.println("The possible values for "+square+" are "+values.get(square));
	}
	
	protected void print_grid()
	{
		String s = "";
		for(int i=0; i<16; ++i)
		{
			if(i%4 == 0)
			{
				s += "\n";
			}
			for(int j=0; j<16; ++j)
			{
				if(j%4 == 0)
				{
					s+="\t";
				}
				s+=template.charAt((16*i)+j);
			}
			s += "\n";
		}
		System.out.println("\n"+s+"\n");
	}
	
	protected boolean is_solved()
	{
		boolean solved = true;
		if(values == null)
		{
			return false;
		}
		for (Rows r : Rows.values())
		{
			for (Cols c : Cols.values())
			{
				if(values.get(r.toString()+c.toString()).length() == 1)
				{
					for(String peer : peers.get(r.toString()+c.toString()))
					{
						solved &= (values.get(r.toString()+c.toString()).charAt(0) != values.get(peer).charAt(0));
					}
				}
				else
				{
					return false;
				}
			}
		}
		return solved;
	}
	
	public static void main(String[] args)
	{
		try 
		{
			PrintStream out = new PrintStream(new FileOutputStream("benchmark.txt"));
			System.setOut(out);
		}
		catch(Exception e)
		{
			System.out.println("Error redirecting standard output: exception " + e + " caught.");
		}
		
		System.out.println("\nSUDOKUHEX SOLVING TEST\n");
		System.out.println("VERSION 1.1\n");
		System.out.println("Backtracking + Constraint Propagation + MRV + Consistency Check\n");
		try
		{
			FileInputStream in = new FileInputStream("all.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			int numSudokus = 0;
			long sumTime = 0;
			long averageTime;
			boolean allSolved = true;
			long sumBacktracks = 0;
			long averageBacktracks;
			long sumConsistencyHeuristic = 0;
			long averageConsistencyHeuristic;
			while ((strLine = br.readLine()) != null)
			{
				System.out.println("\n"+strLine);
				
				// Search benchmark
				long start = (new Date()).getTime();
				
				SudokuHex s = new SudokuHex(strLine);
				s.solve();
				
				long end = (new Date()).getTime();
				
				long time = end - start;
				
				// Results
				//s.template = s.toString();
				//s.print_grid();
				System.out.println(s.toString());
				System.out.println("Is the sudoku solved? "+s.is_solved());
				System.out.println("Time: "+time+" miliseconds.");
				System.out.println("Number of backtracks: " + s.numberBacktracks);
				System.out.println("Times used the consistency heuristic: " + s.timesUsedHeuristic);
				
				++numSudokus;
				sumTime += time;
				allSolved &= s.is_solved();
				sumBacktracks += s.numberBacktracks;
				sumConsistencyHeuristic += s.timesUsedHeuristic;
			}
			averageTime = sumTime/numSudokus;
			averageBacktracks = sumBacktracks/numSudokus;
			averageConsistencyHeuristic = sumConsistencyHeuristic/numSudokus;
			System.out.println("\n\n");
			System.out.println("All sudokus solved successfully: " + allSolved);
			System.out.println(numSudokus + " solved in " + sumTime + "miliseconds.");
			System.out.println("The average sudoku solving time is: "+averageTime + " miliseconds.");
			System.out.println("The average number of backtracks per solving is: "+ averageBacktracks + " backtracks.");
			System.out.println("The average number of times the consistency heuristic is used per solving is: "+ averageConsistencyHeuristic + " times.");
		}
		catch(Exception e)
		{
			System.out.println("Error reading file: exception " + e + " caught.");
		}
	}
}