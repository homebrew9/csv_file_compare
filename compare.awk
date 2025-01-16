# ==============================================================================
# Name  : compare.awk
# Desc  : An awk script to compare two sorted files in a customized fashion.
# Usage : awk -v keys="1,2" -f compare.awk /tmp/file1.sorted.999 /tmp/file2.sorted.999
# By    : prat
# On    : 11/11/2013
# ==============================================================================

BEGIN {
         n = split (keys, a, ",")
         for (i = 1; i<= n; i++)
           k[a[i]] = 1
      }

function process_line(str, action)
{
     sub (/^[><] /, "", str)
     n = split (str, x, ",")
     for (i = 1; i <= n; i++) {
       if ( i in k ) {
         kstr = kstr","x[i]
         line = line","
       } else {
         line = line","x[i]
       }
     }
     sub (/^,/, "", kstr)
     sub (/^,/, "", line)
     if ( action == "load" ) {
       arr[kstr] = line
     }
     else if ( action == "compare" ) {
       # if the key exists in arr array then compare the value tokens
       # otherwise print message
       if ( kstr in arr ) {
         print "KEY : (" kstr ") exists in both files"
         # compare the value tokens
         # delete the key from arr
         delete arr[kstr]
       } else {
         print "KEY : (" kstr ") found only in right"
       }
     }
     kstr = ""
     line = ""
}


NR == FNR {
             process_line($0, "load")
          }

NR != FNR {
             process_line($0, "compare")
          }


# /^[0-9].*[0-9]$/ {
#                    # print NR, " Flush associative arrays; reset toggle switch"
#                    if ( NR > 1 && length(arr) > 0 ) {
#                      print "Length > 0"
#                      delete arr
#                    }
#                    toggle = 0
#                  }
# 
# /^--/ {
#          toggle = 1
#          for ( i in arr )
#            printf ("%-30s  <==>  %s\n", "|"i"|", "|"arr[i]"|")
#       }
# 
# /^</ && toggle == 0 {
#                        process_line($0, "load")
#                     }
# 
# /^</ && toggle == 1 { print NR, "compare left array with right array" }
# 
# /^>/ && toggle == 0 { print NR, "load the right array" }
# 
# /^>/ && toggle == 1 {
#                        process_line($0, "compare")
#                     }
# 
END {
       if ( length(arr) > 0 ) {
         for ( i in arr )
           print "KEY : (" i ") found only in left"
       }
    }

