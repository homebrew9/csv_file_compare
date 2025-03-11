# csv_file_compare
A set of csv file comparison utilities written in various languages: Perl, Python, Ruby, Bash + awk, and Java.

The backstory:
===============
This is a set of csv file comparison utilities that I wrote many years ago.
It started off with Perl, but then I was curious to see how it compares to other languages. So here we are.
The csv comparison programs are in the following languages:
1) Perl (original and modified with optimizations)
2) Python
3) Ruby
4) Bash Shell script
5) Java Swing UI

My findings:
===============
-  Python is (surprisingly) faster than Perl and much cleaner to read. It helped my eventual transition from Perl to Python.
-  Java is a beast! It takes a couple of seconds to compare files that are 100s of MB in size.

# Generic usage
<interpreter> <script_name> -f <csv_file_1,csv_file_2> -k <key_list> -p <precision> -x <exclude_list>

-f <csv_file_1,csv_file_2> : Comma-delimited list of two similar csv files. Relative and absolute paths allowed.
-k <key_list> : Comma-delimited list of columns that form the unique key in each csv file.
-p <precision> : The tolerance for floating-point values. Those that are greater than abs(precision) are considered different.
-x <exclude_list> : Comma-delimited list of columns that should be excluded from comparison.

# Usage of Perl program
perl compare_files.pl -f internet_users_v1.csv,internet_users_v2.csv -k Location -p 0.1

# Usage of modified Perl program
perl compare_files_md.pl -f internet_users_v1.csv,internet_users_v2.csv -k Location -p 0.1

# Usage of Python program
python compare_files.py -f internet_users_v1.csv,internet_users_v2.csv -k Location -p 0.1

# Usage of Ruby program
ruby compare_files.rb -f internet_users_v1.csv,internet_users_v2.csv -k Location -p 0.1

# Usage of Bash shell script
./compare_files.sh -f internet_users_v1.csv,internet_users_v2.csv -k Location -p 0.1

# Usage of Java program
java CompareFiles

- Invoke the Java Swing UI as indicated above.
- Then click on the buttons to specify the file locations, the key, the tolerance, columns to exclude.
- Finally, click on the "Diff" button to view the differences in the bottom grid.
- See the screenshot of the Java GUI program for clarity.

