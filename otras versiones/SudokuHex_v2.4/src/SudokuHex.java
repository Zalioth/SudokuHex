/**
 * @file Grid.java
 * @brief SudokuHex Grid class
 *
 * @version 2.4
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Date;
import java.util.Vector;

/**
 * @brief Hexadecimal Sudoku.
 * Represents and solves a hexadecimal sudoku.
 */
public class SudokuHex 
{	
	private enum Rows
	{
		A("A",0,0), B("B",1,0), C("C",2,0), D("D",3,0), E("E",4,1), F("F",5,1), G("G",6,1), H("H",7,1),
		I("I",8,2), J("J",9,2), K("K",10,2), L("L",11,2), M("M",12,3), N("N",13,3), O("O",14,3), P("P",15,3);
		
		private final String s;
		private final int row;
		private final int superRow;
		
		Rows(String s, int row, int superRow)
		{
			this.s = s;
			this.row = row;
			this.superRow = superRow;
		}
		
		public String toString()
		{
			return s;
		}
		
		public int row()
		{
			return row;
		}
		
		public int superRow()
		{
			return superRow;
		}
	}
	
	private enum Cols
	{
		a("0",0,0), b("1",1,0), c("2",2,0), d("3",3,0), e("4",4,1), f("5",5,1), g("6",6,1), h("7",7,1), 
		i("8",8,2), j("9",9,2), k("A",10,2), l("B",11,2), m("C",12,3), n("D",13,3), o("E",14,3), p("F",15,3);
		
		private final String s;
		private final int column;
		private final int superColumn;
		
		Cols(String s, int column, int superColumn)
		{
			this.s = s;
			this.column = column;
			this.superColumn = superColumn;
		}
		
		public String toString()
		{
			return s;
		}
		
		public int column()
		{
			return column;
		}
		
		public int superColumn()
		{
			return superColumn;
		}
	}
	
	private final int GRID_SIZE = 16;
	private final int NUM_PEERS = 39;
	private final int NUM_UNITS = 3;
	private String template; /**< Text template that represents a hexadecimal sudoku grid (0-F for values set values, and . or - for free squares. It can also contain carriage returns, tabulations or whitespaces to increase readability). */
	// STATIC DICTIONARIES
	private int[][][][][] units; /**< */
	private int[][][][] peers; /**< */
	// DYNAMIC DICTIONARY
	private String[][] values; /**< */
	
	private int numberBacktracks; /**< */
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
		prepare_template(template);
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
	 * @brief Initializes the dictionaries that will represent the units and peers of a square.
	 */
	private void initialize_units_and_peers()
	{
		units = new int[GRID_SIZE][GRID_SIZE][NUM_UNITS][GRID_SIZE][2];
		peers = new int[GRID_SIZE][GRID_SIZE][NUM_PEERS][2];
		
		for(int r=0; r<GRID_SIZE; ++r)
		{
			for(int c=0; c<GRID_SIZE; ++c)
			{
				// For each square...
				int peer = 0;
				int unit1Item = 0;
				int unit2Item = 0;
				int unit3Item = 0;
				for(int k=0; k<GRID_SIZE; ++k)
				{
					// Initialize units
					// Add the row
					units[r][c][0][unit1Item][0] = r;
					units[r][c][0][unit1Item][1] = k;
					++unit1Item;
					// Add the column
					units[r][c][1][unit2Item][0] = k;
					units[r][c][1][unit2Item][1] = c;
					++unit2Item;
					// Add the box
					int superRow = (int)r/4;
					int superColumn = (int)c/4;
					int boxRow = (int)k/4;
					int boxColumn = k%4;
					units[r][c][2][unit3Item][0] = 4*superRow + boxRow;
					units[r][c][2][unit3Item][1] = 4*superColumn + boxColumn;
					++unit3Item;
					
					// Initialize peers
					// (Same as the units, but without duplicates, and without the square [i,j])
					// Add the row
					if(k!=c)
					{
						peers[r][c][peer][0] = r;
						peers[r][c][peer][1] = k;
						++peer;
					}
					// Add the column
					if(k!=r)
					{
						peers[r][c][peer][0] = k;
						peers[r][c][peer][1] = c;
						++peer;
					}
					// Add the box
					if( (4*superRow + boxRow != r) && (4*superColumn + boxColumn != c) )
					{
						peers[r][c][peer][0] = 4*superRow + boxRow;
						peers[r][c][peer][1] = 4*superColumn + boxColumn;
						++peer;
					}
				}
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
		values = new String[16][16];
		int templateIndex = 0;
		String all_possible_values = "0123456789ABCDEF";
		
		for(int r=0; r<GRID_SIZE; ++r)
		{
			for(int c=0; c<GRID_SIZE; ++c)
			{
				// The value from the template will be read, and it will be added to the values set.
				// If the value read is the empty symbol (. or -) then all the possible values will be added to the set.
				if(template.charAt(templateIndex) == '.')
				{
					// Free square, add all the possible values
					values[r][c] = all_possible_values;
				}
				else
				{
					// Established square, add only the read value
					String single_value = "" + template.charAt(templateIndex);
					values[r][c] = single_value;
				}
				++templateIndex;
			}
		}
		
		// Now that the values are set, the data structure must be taken to a consistent state, hence later,
		// when propagating the restrictions and backtracking, the decisions made are also consistent.
		for (int r=0; r<GRID_SIZE; ++r)
		{
			for (int c=0; c<GRID_SIZE; ++c)
			{
				// For each square, if it has only one possible value (if it is set), remove the value from its peers.
				if(values[r][c].length() == 1)
				{
					for(int peer=0; peer<NUM_PEERS; ++peer)
					{
						int peerRow = peers[r][c][peer][0];
						int peerColumn = peers[r][c][peer][1];
						values[peerRow][peerColumn] = values[peerRow][peerColumn].replace(values[r][c], "");
					}
				}
			}
		}
	}
	
	/**
	 * @brief Solves the sudoku and changes the values content to a solved state.
	 * @post The values will be changed to a state belonging to the solution set. If no solution was found, they will be null.
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
	private String[][] search(String[][] values)
	{
		if(values == null)
		{
			// Already failed...
			return null;
		}
		// Check goal condition (if the sudoku is solved) 
		// and keep track of the node that minimizes the branching factor (the square with minimal value choices (different from zero)).
		boolean solved = true;
		int bestSquareRow = 0;
		int bestSquareColumn = 0;
		int bestSquareDegree = -1;
		
		for (int r=0; r<GRID_SIZE; ++r)
		{
			for (int c=0; c<GRID_SIZE; ++c)
			{
				if(values[r][c].length()==0)
				{
					// Contradiction! The test square is invalid!
					return null;
				}
				else
				{
					// Update the goal condition flag.
					solved &= (values[r][c].length() == 1);
					
					// Update the best square.
					if(values[bestSquareRow][bestSquareColumn].length() == 1)
					{
						// The best square is useless (only one possible value, no decision to make)
						// Pick the first one that doesn't have only one value.
						if(values[r][c].length() > 1)
						{
							bestSquareRow = r;
							bestSquareColumn = c;
						}
					}
					else if(values[r][c].length() < values[bestSquareRow][bestSquareColumn].length() && values[r][c].length() > 1)
					{
						// Assign the test square if it's better (already tested that it's valid).
						bestSquareRow = r;
						bestSquareColumn = c;
					}
				}
			}
		}
		
		//System.out.println("Found best square, ["+bestSquareRow+","+bestSquareColumn+"] with degree "+bestSquareDegree);
		
		if(solved)
		{
			// Solved!
			return values;
		}
		else
		{
			// Not solved, pick the tree with least branches from our forest of solution trees.
			// The possible values for the square are chosen using the LCV (Least Constrainted Value)
			// For each possible value, it's checked how many times it appears in the square's peers,
			// The value that appears the least is chosen.
			// This works because the value chosen will minimize the constraints imposed over other squares,
			// thus, increasing the chances of generating a valid solution. 
			String[][] originalValues = copy_values(values);
			
			String temp = originalValues[bestSquareRow][bestSquareColumn];
			class Pair
			{
				char c;
				int i;
				Pair(char c, int i)
				{
					this.c = c;
					this.i = i;
				}
			};
			
			Vector<Pair> unorderedValues = new Vector<Pair>();
			for(int i=0; i<temp.length(); ++i)
			{
				int j = 0;
				for(int peer=0; peer<NUM_PEERS; ++peer)
				{
					if(values[peers[bestSquareRow][bestSquareColumn][peer][0]][peers[bestSquareRow][bestSquareColumn][peer][1]].contains(""+temp.charAt(i)))
					{
						++j;
					}
				}
				unorderedValues.add(new Pair(temp.charAt(i),j));
			}
			String orderedValues = "";
			for(int j=unorderedValues.size(); j>0; --j)
			{
				int index = 0;
				for(int i=0; i<unorderedValues.size(); ++i)
				{
					if(unorderedValues.get(i).i < unorderedValues.get(index).i)
					{
						index = i;
					}
				}
				orderedValues += unorderedValues.get(index).c;
				unorderedValues.remove(index);
			}
			
			for(int i = 0; i < orderedValues.length(); i++)
			{
				// Assign it to the square
				values = assign(values, bestSquareRow, bestSquareColumn, orderedValues.charAt(i));
				// Search depth-first
				String[][] obtainedValues = search(values);
				// If the value returned is a solution (not a null object), solved!
				if(obtainedValues != null)
				{
					return obtainedValues;
				}
				numberBacktracks++;
				// If the value returned is null, the sub-branch doesn't have a solution, try the next value!
				// Before trying another value, the original values must be restored.
				values = copy_values(originalValues);
			}
			// If after checking all the values, none provides a solution, this branch doesn't have a solution, backtrack!
			return null;
		}
	}
	
	/**
	 * @brief Returns a copy of the matrix of values passed as an argument.
	 * @param values Matrix of Strings that contain the values of each square.
	 * @return Copy of the Matrix of values.
	 */
	private String[][] copy_values(String[][] values)
	{
		String [][] copy = new String[GRID_SIZE][GRID_SIZE];
		for (int r=0; r<GRID_SIZE; ++r)
		{
			for (int c=0; c<GRID_SIZE; ++c)
			{
				// Strings are immutable objects, there's no need to deep-copy them.
				// Copying the references will do.
				copy[r][c] = values[r][c];
			}
		}
		return copy;
	}
	
	/**
	 * @brief
	 * @param values
	 * @param square
	 * @param value
	 * @return
	 */
	private String[][] assign(String[][] values, int row, int column, char value)
	{
		// Eliminate all values different to value
		// If they all succeed, return the values data structure
		// otherwise, return null
		String valuesToRemove = values[row][column].replace(""+value, "");
		
		//System.out.println("Assigning "+value+" to ["+row+","+column+"] (its values are "+values[row][column]+")");
		
		for(char v : valuesToRemove.toCharArray())
		{
			values = eliminate(values, row, column, v);
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
	private String[][] eliminate(String[][] values, int row, int column, char value)
	{
		if(!values[row][column].contains(""+value))
		{
			// Already eliminated
			return values;
		}
		
		// Eliminate the value
		String temp = values[row][column].replace(""+value, "");
		if(temp.isEmpty())
		{
			// Contradiction, attempting to remove the last value!
			return null;
		}
		values[row][column] = temp;
		
		// If now that the value was eliminated, we find that there's only one possible value
		// left for this square (same effect as assigning the value to this square)...
		// propagate the restrictions, eliminating this new found value through the square's peers
		// If any of the eliminations returns null, it means it couldn't be done, and we have to propagate the failure
		// up in the tree and backtrack.
		if(temp.length() == 1)
		{
			for(int peer=0; peer<NUM_PEERS; ++peer)
			{
				values = eliminate(values, peers[row][column][peer][0], peers[row][column][peer][1] , temp.charAt(0));
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
		
		Vector<int[]> toCheck = new Vector<int[]>();
		
		for(int unit=0; unit<NUM_UNITS; ++unit)
		{
			// For each square of each unit (row, column and box) which the square belongs to...
			for(int i=0; i<GRID_SIZE; ++i)
			{
				int r = units[row][column][unit][i][0];
				int c = units[row][column][unit][i][1];
				if(values[r][c].contains(""+value))
				{
					int[] s = new int[2];
					s[0] = r;
					s[1] = c;
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
			return assign(values, toCheck.firstElement()[0], toCheck.firstElement()[1], value);
		}
		else
		{
			// More than one option, no choice is made here, that's the backtracking search's responsibility.
			
			// Before returning, a final consistency check will be made.
			// Sudoku is declared as an AllDiff constraint. We can check in advance
			// for a valid solution by doing the following simple form of
			// inconsistency detection: if there are M variables involved in the
			// constraint and if they have N possible distinct values altogether,
			// and M>N, then the constraint cannot be satisfied (and we don't have
			// to spend time on this branch that will eventually go wrong).

			for (int unit = 0; unit < NUM_UNITS; ++unit)
			{
				int m = 0;
				int n = 0;
				String possibleValues = "";
				// For each square of each unit (row, column and box) which the
				// square belongs to...
				for (int i = 0; i < GRID_SIZE; ++i)
				{
					++m;
					for (char v : values[units[row][column][unit][i][0]][units[row][column][unit][i][1]]
							.toCharArray())
					{
						if (!possibleValues.contains("" + v))
						{
							++n;
							possibleValues += v;
						}
					}
				}
				if (m > n)
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
		for (int r=0; r<GRID_SIZE; ++r)
		{
			for (int c=0; c<GRID_SIZE; ++c)
			{
				s += values[r][c];
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
		//System.out.println(template);
		
		for (Rows r : Rows.values())
		{
			for (Cols c : Cols.values())
			{
				System.out.print("\t"+r.toString()+c.toString()+"\t");
				System.out.print(values[r.row()][c.column()]);
			}
			System.out.println("\n");
		}
		
		
	}
	
	protected void print_values()
	{
		System.out.println();
		for (Rows r : Rows.values())
		{
			for (Cols c : Cols.values())
			{
				System.out.print("\t"+r.toString()+c.toString()+"\t");
				System.out.print(values[r.row()][c.column()]);
			}
			System.out.println("\n");
		}
		System.out.println();
	}
	
	protected void print_values(String[][] values)
	{
		System.out.println();
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
				System.out.print(values[r.row()][c.column()]);
			}
			System.out.println("\n");
		}
		System.out.println();
	}
	
	protected void show_values(String[][] values, int row, int column)
	{
		if(values == null)
		{
			System.out.println("null values returned");
			return;
		}
		System.out.println("The possible values for ["+row+","+column+"] are "+values[row][column]);
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
		for (int r=0; r<GRID_SIZE; ++r)
		{
			for (int c=0; c<GRID_SIZE; ++c)
			{
				if(values[r][c].length() == 1)
				{
					for(int peer=0; peer<NUM_PEERS; ++peer)
					{
						solved &= (values[r][c].charAt(0) != values[peers[r][c][peer][0]][peers[r][c][peer][1]].charAt(0));
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
	
	public static void main1(String[] args) throws FileNotFoundException, IOException
	{
		//FileInputStream in = new FileInputStream("all.txt");
		//BufferedReader br = new BufferedReader(new InputStreamReader(in));
		//String strLine = "....................1846D5A3FBE04DB572EAF96018C330A6FD5B18CE7249573A0F62C194BD8E614CD7938EB20F5A8FEB5AC407D63192920D81BE3F5AC764A5736CF82B19E40DB6149375ED082AFCC8F0AE2D4375691BD92EB4106AFC8375F357EB0C9421A6D8E46135A7BC8D902F0AC829DF56374EB12BD94681A0EF5C37";
		String strLine = ".E8F..39724BD5.67C9.....D.A...E0..B...E...601.C..0.6F.......7.......0F6...94BD.....C..9.8..20.5A8....A.4.7..31..92..8..E3.5....4A.736CF82B..E.0DB61493..E..82..CC8..A..D4..56.1..9.....06.F.8..5F...EB0..42.A6.8E..1.5A.B.8D90.F...8.9.F.6...EB1..D..681..E.....";
		
		int numSudokus = 0;
		long sumTime = 0;
		long averageTime;
		boolean allSolved = true;
		long sumBacktracks = 0;
		long averageBacktracks;
		//strLine = br.readLine();
		
		System.out.println("\n" + strLine);

		// Search benchmark
		long start = (new Date()).getTime();

		SudokuHex s = new SudokuHex(strLine);
		s.solve();

		long end = (new Date()).getTime();

		long time = end - start;

		// Results
		// s.template = s.toString();
		// s.print_grid();
		System.out.println(s.toString());
		System.out.println("Is the sudoku solved? " + s.is_solved());
		System.out.println("Time: " + time + " miliseconds.");
		System.out.println("Number of backtracks: " + s.numberBacktracks);

		++numSudokus;
		sumTime += time;
		allSolved &= s.is_solved();
		sumBacktracks += s.numberBacktracks;
		
		averageTime = sumTime/numSudokus;
		averageBacktracks = sumBacktracks/numSudokus;
		System.out.println("\n\n");
		System.out.println("All sudokus solved successfully: " + allSolved);
		System.out.println(numSudokus + " solved in " + sumTime + "miliseconds.");
		System.out.println("The average sudoku solving time is: "+averageTime + " miliseconds.");
		System.out.println("The average number of backtracks per solving is: "+ averageBacktracks + " backtracks.");
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
		System.out.println("VERSION 2.4\n");
		System.out.println("Backtracking + Constraint Propagation + MRV + LCV + Consistency Check\n");
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