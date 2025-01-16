# $Id: process_diff.awk 27011 2013-12-02 16:15:09Z amkjpf $
# =========================================================================================================
# Name    : process_diff.awk
# Desc    : An awk wrapper around the diff command to process further and return customized output.
#           In awk, any nonzero numeric value or any nonempty string value is TRUE.
#           Any other value (zero or the null string, "") is FALSE.
# Usage   : awk -v keys=<key_list> -v files=<file_list> -v tolerance=N -v header=<header_columns> \
#               -v exclude=<exclude_list> -f process_diff.awk diff_file
# Example : awk -v keys="FUNDCODE,FUNDNAME" \
#               -v files="sybase.csv,oracle.csv" \
#               -v tolerance="0.001" \
#               -v header="FUNDNAME,FUNDCODE,CAPITAL,INTEREST,AMOUNT" \
#               -v exclude="INTEREST,AMOUNT" \
#               -f process_diff.awk \
#               files.diff
# By      : prat
# On      : 11/5/2013
# =========================================================================================================

BEGIN {
         # passed variables = keys, files, tolerance, header
         c = split (header, cols, ",")                       # split header into array cols
         n = split (keys, a, ",")                            # split keys into array a
         for (i = 1; i<= n; i++)                             # loop through keys
           for (j = 1; j<=c; j++)                            # for each key, loop through columns
             if ( a[i] == cols[j] ) { k[j] = 1; break }      # if it is a key column, set associative array k; break
                                                             # so now k looks like: k[1]=1, k[2]=1 because positions 1,2 are keys
         m = split (exclude, b, ",")                         # split exclude columns into array b
         for (i = 1; i<= m; i++)                             # loop through exclude columns
           for (j = 1; j<=c; j++)                            # for each exclude column, loop through header columns
             if ( b[i] == cols[j] ) { ex[j] = 1; break }     # if it is an exclude column, set associative array ex; break
                                                             # so now ex looks like: ex[4]=1, k[5]=1 because positions 4,5 are keys
         file["<"] = substr(files, 1, index(files, ",")-1)   # set the left file in the associative array "file"
         file[">"] = substr(files, index(files, ",") + 1)    # set the right file in the associative array "file"
      }

function abs(x) { return x >= 0 ? x : -x }

function isnum(x) { return x == x + 0 }

function is_unequal(value1, value2)
{
   if ( isnum(value1) && isnum(value2) )                # if both values to compare are numbers
   {
      if ( abs(value1 - value2) >= tolerance )          # and their absolute difference is greater than the tolerance
        return 1                                        # then they are unequal
   }
   else if (value1 != value2)                           # if the string values are different
     return 1                                           # then they are unequal
   return 0                                             # for every other case, they are equal
}

function compare_lines(key, line1, line2)
{
   n1 = split(line1, a1, ",")
   n2 = split(line2, a2, ",")
   for (i = 1; i<= n1; i++) {
     if ( is_unequal(a1[i], a2[i] ) )                   # if a1[i] not equal to a2[i] then add column name
       mismatches = mismatches","cols[i]":"i            # and position to mismatch string
   }
   if ( length(mismatches) > 0 ) {
     sub (/^,/, "", mismatches)
     printf ("KEY: %-30s => Mismatched columns = %s\n", "("key")", mismatches)
     mismatches = ""
   }
}

function process_line(str, action)
{
     side = substr(str, 1, 1)                                                # determine the side specified by the diff output
     sub (/^[><] /, "", str)                                                 # "<" = file on left side; ">" = file on right side
     n = split (str, x, ",")
     for (i = 1; i <= n; i++) {                                              # split line and form the key and value strings
       if ( i in k ) {
         kstr = kstr","x[i]
         line = line","                                                      # remove key column from the line to compare
       } else if (i in ex) {
         line = line","                                                      # also remove exclude column from the line to compare
       } else {
         line = line","x[i]                                                  # all other columns are in the line to compare
       }
     }
     sub (/^,/, "", kstr)
     sub (/^,/, "", line)
     if ( action == "load" ) {                                               # if the action is to load then set the key/value pair
       arr[side, kstr] = line                                                # arr is a multidimensional array, it's key is "<" SUBSEP "key_cols"
     }
     else if ( action == "compare" ) {                                       # if the action is to compare then
       tside = side == ">" ? "<" : ">"
       if ( (tside, kstr) in arr ) {                                         # check if the key exists in array arr
         compare_lines( kstr, line, arr[tside, kstr] )                       # The key was found in both files; compare the value tokens
         delete arr[tside, kstr]                                             # delete the key from arr
       } else {
         printf ("KEY: %-30s => Found only in %s\n", "("kstr")", file[side]) # if key was not found in arr, it exists only in one file
       }
     }
     kstr = ""
     line = ""
}

/^[0-9].*[0-9]$/ {
                    if ( NR > 1 && length(arr) > 0 ) {          # if diff's line status: "m[,n][acd]p[,q]" is seen then
                      delete arr                                # flush the array arr and start over
                    }
                    toggle = 0                                  # and set the toggle switch off
                 }

/^--/ {                                                         # if diff's line separator is seen then set the toggle switch on
         toggle = 1                                             # which means the array has been loaded and now compare the file records with the
      }                                                         # contents of the array

/^[><]/ && toggle == 0 {
                          process_line($0, "load")              # if toggle is off, load the array arr
                       }

/^[><]/ && toggle == 1 {
                          process_line($0, "compare")           # if the toggle is on, compare the file records with the array arr
                       }

END {
       if ( length(arr) > 0 ) {                                 # if anything remains in the array at the end, it was found only in one file
         for ( i in arr ) {
           split (i, separate, SUBSEP)
           printf ("KEY: %-30s => Found only in %s\n", "("separate[2]")", file[separate[1]])
         }
         delete arr
       }
    }

