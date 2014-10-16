#SudokuHex

##Description
Software that solves hexadecimal sudokus. Furthermore, several implementations (all Constraint Propagation based, with heuristics or efficiency improvements added) are provided and compared.

##Methods and heuristics used
###Constraint propagation.
Well stablished method to find a solution to a problem that can be expressed as a CSP (Constraint Satisfaction Problem). Used in all versions. Initial versions use less efficient, faster to develop with, data structures. Latest versions use arrays, which are harder to work with, but more efficient.
###Most Restrained Variable.
Heuristic that consists on choosing the most restrained variable. It aims to minimize the branching factor.
###Least Constraining Value.
Heuristic that consists on choosing the least constraining value (once the variable is fixed). It aims to evaluate first the branches that lead to least constrained situations, which will allow more freedom to generate valid solutions.
###Consistency Check.
Heuristic that consists on checking the branch consistency (potential correctness of the solution solution being developed). It aims to prune branches that won't lead to valid solutions as soon as possible.

##Documentation and Efficiency comparison
Please read the file sudokuhex.pdf (in Spanish).
