package fr.telecom_paristech.dbweb.regexrepair.simple;

/**
 * Computes a new cell score
 */
public class UpdateFunction {

  public int score(MyerMatrix.Cell currentCell, MyerMatrix.Cell predecessorCell, Regex leaf) {

    // Get a bonus for each new matched character
    int score = (currentCell.numMatchedCharacters + currentCell.numMatchedSpecialCharacters * 1000) * 1000;

    // Skips have to live in a separate ordcer of magnitude, otherwise
    // $4-1$/$4-a$ will not work.

    // Punish any skipped characters in the regex
    score -= currentCell.skips;

    // Give preference to smaller j's
    // TODO score -= predecessorCell.j;

    // Give preference to direct characters
    //score += leaf.type == NodeType.CHARACTER ? 1000 : 0;

    // Giving preference to less disjunctions
    // destroys the course number example
    //score+=(10-leaf.numberOfDisjunctions())*10000;

    // Giving preference to connected components does not help
    /*result.numComponents = cell.numComponents;
		if (i != cell.previousMatchedChar + 1)
			result.numComponents++;
		result.previousMatchedChar = i;*/
    //result.previousI = i;
    //result.previousJ = j;
    return (score);
  }
}
