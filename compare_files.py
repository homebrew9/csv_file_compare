#!/usr/bin/env python
# $Id: compare_files.py 27007 2013-12-02 15:53:34Z amkjpf $
# ==============================================================================
# Name    : compare_files.py
# Desc    : A Python program to compare two csv files in a highly customized
#           fashion.
# Usage   : python compare_files.py -f file1.csv,file2.csv -k <key_list> \
#                                   -p <precision> -x <exclude_list>
# Example : python compare_files.py -f sybase.csv,oracle.csv \
#                                   -k FUNDCODE,FUNDNAME -p 0.01 -x CAPITAL
# By      : prat
# On      : 11/18/2013
# ==============================================================================
import sys
import optparse
keycols = {}    # a dictionary for key columns; key=column_name, value=Found/Not_Found
exclude = {}    # a dictionary for exclude columns; key=column_name, value=Found/Not_Found
hdr = []        # a list for all columns in the header
datadict = {}   # a dictionary for all data lines in one file

# ==============================================================================
# ERROR CODES
# ==============================================================================

e_invalid_args = 1
e_null_key = 2
e_null_exclude = 3
e_key_exclude_overlap = 4
e_key_not_found = 5
e_exclude_not_found = 6
e_headers_mismatch = 7

# The columns of the header line are elements of the array hdr and are objects of
# the following class HeaderCol. A header column has a name, and is either a key or
# an exclude column.
class HeaderCol:
    def __init__(self,name):
        self.name = name
        self.key = False
        self.exclude = False
    def setkey(self):
        self.key = True
    def setexclude(self):
        self.exclude = True

# ==============================================================================
# FUNCTION SECTION
# ==============================================================================

# A function to print the usage of this script
def check_usage():
    if len(sys.argv) == 1:
        print "Usage: python compare_files.py -f file1,file2 -k <KEY> -p <N>"
        print "       <KEY> = comma-delimited list of key columns"
        print "       <N>   = integer degree of precision sought for comparison"
        sys.exit(e_invalid_args)

def process_args():
    global file1, file2, precision
    p = optparse.OptionParser()
    # An option taking an argument
    p.add_option("-f", action="store", dest="file_list")
    p.add_option("-k", action="store", dest="key_list")
    p.add_option("-p", action="store", dest="precision")
    p.add_option("-x", action="store", dest="exclude_list")
    # Parse the command line
    opts, args = p.parse_args()
    # Retrieve the option settings
    (file1, file2) = opts.file_list.split(",")
    for i in opts.key_list.split(","):
        if i == "":
            print "At least one key column is a null string. Abnormal exit."
            sys.exit(e_null_key)
        keycols[i] = "Not_Found"  # assume that key column was not seen in header
    precision = opts.precision
    if precision == None:
        precision = 0.01
    if opts.exclude_list != None:
        for i in opts.exclude_list.split(","):
            if i == "":
                print "At least one exclude column is a null string. Abnormal exit."
                sys.exit(e_null_exclude)
            if i in keycols.keys():
                print "Keys and exclude columns overlap. Abnormal exit."
                sys.exit(e_key_exclude_overlap)
            exclude[i] = "Not_Found" # assume that exclude column was not seen in header

# Read the header line of one file and check that all key columns and exclude columns
# were present in the header
def read_header(file):
    f = open(file)
    line = f.readline()
    f.close()
    for i in line.rstrip("\r\n").split(","):
        a = HeaderCol(i)
        if i in keycols:
            a.setkey()
            keycols[i] = "Found"
        if i in exclude:
            a.setexclude()
            exclude[i] = "Found"
        hdr.append(a)
    # All key columns and exclude columns must be found in the header list
    for item in keycols:
        if keycols[item] == "Not_Found":
            print "Key column: ", item, "was not found in the header. Abnormal exit."
            sys.exit(e_key_not_found)
    for item in exclude:
        if exclude[item] == "Not_Found":
            print "Exclude column: ", item, "was not found in the header. Abnormal exit."
            sys.exit(e_exclude_not_found)

# Read the header of the second file and check that the names and order of the header
# columns match that of the first.
def compare_headers(file):
    f = open(file)
    line = f.readline()
    f.close()
    j = 0
    for i in line.rstrip("\r\n").split(","):
        if j >= len(hdr):
            print "The headers of the two files do not match. Abnormal exit."
            sys.exit(e_headers_mismatch)
        if i != hdr[j].name:
            print "The headers of the two files do not match. Abnormal exit."
            sys.exit(e_headers_mismatch)
        j += 1
    if j < len(hdr):
        print "The headers of the two files do not match. Abnormal exit."
        sys.exit(e_headers_mismatch)

# Read the data of the first file and store it in a hash called datadict. The key is a
# comma-delimited string of the key column values. The value is the line of data read,
# with keys and exclude columns replaced by zero-length strings.
def read_data(file):
    f = open(file)
    first_line = True
    for line in f:
        if first_line == True:
            first_line = False
            continue
        key = ""
        arr = []
        for (j, k) in enumerate(line.rstrip("\r\n").split(",")):
            if j >= len(hdr):
                continue
            if hdr[j].key == True:
                key += "," + k
                arr.append("")
            elif hdr[j].exclude == True:
                arr.append("")
            else:
                arr.append(k)
        key = key.lstrip(",")
        datadict[key] = arr
    f.close

# Iterate through the second file and compare it against the hash datadict.
# If a key is found, compare the non-key columns and print mismatched columns, if found.
# If a key is not found, print the appropriate message.
# Finally, check if a key was found in the first file but not in the second.
def compare_data(iter_file, ref_file):
    # iter_file = file we are iterating through
    # ref_file = file we are referencing against
    f = open(iter_file)
    first_line = True
    for line in f:
        if first_line == True:
            first_line = False
            continue
        key = ""
        arr = []
        for (j, k) in enumerate(line.rstrip("\r\n").split(",")):
            if j >= len(hdr):
                continue
            if hdr[j].key == True:
                key += "," + k
                arr.append("")
            elif hdr[j].exclude == True:
                arr.append("")
            else:
                arr.append(k)
        key = key.lstrip(",")
        if key in datadict:
            # the key was found in both files; compare their non-key data
            mismatches = compare_arrays(arr, datadict[key])
            if mismatches != "":
                print "KEY: %-30s => Mismatched columns = %s" % ("("+key+")", mismatches)
            del datadict[key]
        else:
            print "KEY: %-30s => Found only in %s" % ("("+key+")", iter_file)
    f.close
    for k in datadict:
        print "KEY: %-30s => Found only in %s" % ("("+k+")", ref_file)

# A utility function to compare arrays and return a comma-delimited list of mismatched
# column names and positions.
def compare_arrays(arr1, arr2):
    mm = ""
    for (i, elem) in enumerate(arr1):
        if are_unequal(elem, arr2[i]):
            mm += "," + hdr[i].name + ":" + str(i+1)
    mm = mm.lstrip(",")
    return mm

def are_unequal(value1, value2):
    # Returns True if both values are unequal, False if equal
    # If both values are numbers, then compare them as numbers
    # with values acceptable within degree of precision; otherwise
    # compare them as strings
    if is_num(value1) and is_num(value2):
        diff = abs(float(value1) - float(value2))
        if float(diff) >= float(precision):
            return True
        else:
            return False
    else:
        return value1 != value2

# A function to determine if the parameter is a number or not
def is_num(s):
   try:
       float(s)
       return True
   except ValueError:
       return False

# ==============================================================================
# MAIN SECTION
# ==============================================================================
check_usage()
process_args()
read_header(file1)
compare_headers(file2)
read_data(file1)
compare_data(file2, file1)

