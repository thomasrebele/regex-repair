# regex-repair
This project aims to improve a regular expression by adding a set of missing (non-matched) words.
Two algorithms were developed with this goal.

## Basic algorithm

This algorithm is described in [1].
It introduces new disjunctions `...(...|missing-substr)...` where necessary.
It recursively descends into the regex tree, and applies the changes.
[Try it online](https://www.thomasrebele.org/projects/regex-repair/demo)!

## Adaptive algorithm

This is an extension of the basic algorithm, described in [2].
The basic algorithm might remove parts of the regex that are essential to its meaning.
To avoid this problem, the adaptive version checks intermediate regexes, and only accepts them, if their quality is good enough.
The quality can be determined (amongst others) by a test-set which contains only negative examples, but which is similar to the documents that the regex will be applied on.

## Datasets

Datasets can be found at (https://www.thomasrebele.org/projects/regex-repair/)

## License

This project is licensed under the GNU AFFERO GENERAL PUBLIC LICENSE (AGPL) v3, see LICENSE for more details.
If you need a different license, please contact us.

# References

[1] Rebele, T., Tzompanaki, K., Suchanek, F.: Visualizing the addition of missing words to regular expressions. In: International Semantic Web Conference, ISWC (2017)

[2] Rebele, T., Tzompanaki, K., Suchanek, F.: Adding Missing Words to Regular Expressions. In: Pacific-Asia Conference on Knowledge Discovery and Data Mining, PAKDD (2018)

