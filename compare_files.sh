#!/usr/bin/bash
# $Id: compare_files.sh 26650 2013-11-08 23:00:10Z amkjpf $
# ===============================================================================================================
# Name    : compare_files.sh
# Desc    : A shell script to compare two csv files. The script does parameter value checks and then sorts the
#           two files based on the sort key. Thereafter a diff is done and the control is passed to an awk
#           script that works as a wrapper over the diff output.
# Usage   : ./compare_files.sh -f file1,file2 -k <key_list> -p N -x <exclude_list>
#           <key_list>     = comma-delimited list of key columns
#           N              = the tolerance required
#           <exclude_list> = comma-delimited list of columns to be excluded
# Example : ./compare_files.sh -f pl_syb_20131025.csv,pl_ora_20131025.csv -k PolNum -p 0.01
# By      : prat
# On      : 11/7/2013
# ===============================================================================================================
#set -vx

# ==============================================================================
# ERROR CODES
# ==============================================================================
E_INVALID_ARGS=1
E_KEY_ABSENT=2
E_EXCLUDE_ABSENT=3
E_KEY_EXCLUDE_OVERLAP=4
E_INVALID_FILE=5
E_INVALID_FILE_ARG=6
E_INVALID_KEY_COLUMN=7
E_NO_KEY_COLUMNS=8
E_INVALID_EXCLUDE_COLUMN=9
E_MISMATCHED_HEADERS=10

# ==============================================================================
# SUBROUTINE SECTION
# ==============================================================================
check_usage() {
  if [ $# == 0 ]
  then
    echo "Usage: $(basename $0) -f file1,file2 -k <key_list> -p N -x <exclude_list>"
    echo "       <key_list>     = comma-delimited list of key columns"
    echo "       N              = the tolerance required"
    echo "       <exclude_list> = comma-delimited list of columns to be excluded"
    exit $E_INVALID_ARGS 
  fi
}

process_args() {
  while getopts ":f:k:p:x:" opt; do
    case $opt in
      f) filestr=$OPTARG
         file1=${OPTARG%%,*}
         file2=${OPTARG##*,}
         ;;
      k) keystr=$OPTARG
         OLDIFS=$IFS
         IFS=","
         i=0
         for k in $OPTARG; do
           key[$i]=$k
           ((i++))
         done
         IFS=$OLDIFS
         ;;
      p) tolerance=$OPTARG
         ;;
      x) excludestr=$OPTARG
         OLDIFS=$IFS
         IFS=","
         i=0
         for x in $OPTARG; do
           exclude[$i]=$x
           ((i++))
         done
         IFS=$OLDIFS
         ;;
     \?) echo "Invalid option: -$OPTARG"
         exit $E_INVALID_ARGS
         ;;
      :) echo "Option -$OPTARG requires an argument"
         exit $E_INVALID_ARGS
         ;;
    esac
  done
  # sanity check for all input parameters
  if [ -z "$file1" ]; then
    echo "One of or both the file parameters is a null string: |$file1| or |$file2|. Abnormal exit."
    exit $E_INVALID_FILE_ARG
  elif [ -z "$file2" ]; then
    echo "One of or both the file parameters is a null string: |$file1| or |$file2|. Abnormal exit."
    exit $E_INVALID_FILE_ARG
  elif [ ! -s $file1 ]; then
    echo "Either the file does not exist or it is of zero bytes: $file1. Abnormal exit."
    exit $E_INVALID_FILE
  elif [ ! -s $file2 ]; then
    echo "Either the file does not exist or it is of zero bytes: $file2. Abnormal exit."
    exit $E_INVALID_FILE
  elif [ "${#key[@]}" == 0 ]; then
    echo "No key columns specified. Abnormal exit."
    exit $E_NO_KEY_COLUMNS
  elif [ "${#key[@]}" > 1 ]; then
    for i in "${key[@]}"; do
      if [ -z "$i" ]; then
        echo "At least one key column is a null string. Abnormal exit."
        exit $E_INVALID_KEY_COLUMN
      fi
    done
  fi
  # if unspecified, set tolerance to 0.01
  if [ -z "$tolerance" ]; then
    tolerance=0.01
  fi
  # if exclude columns are specified, check that they are all non null strings
  if [ "${#exclude[@]}" > 1 ]; then
    for i in "${exclude[@]}"; do
      if [ -z "$i" ]; then
        echo "At least one exclude column is a null string. Abnormal exit."
        exit $E_INVALID_EXCLUDE_COLUMN
      fi
    done
  fi
}

read_header() {
  # Read the header of file1
  hdr=$(sed -n 1p $file1)          # header string
  OLDIFS=$IFS                      # header array
  IFS=","
  i=0
  for h in $hdr; do
    hdr_arr[$i]=$h
    ((i++))
  done
  IFS=$OLDIFS
}

contains_element() {
  local e
  for e in "${@:2}"; do
    [[ "$e" == "$1" ]] && return 0
  done
  return 1
}

check_for_key_cols() {
  for i in "${key[@]}"; do
    # if the key column is not in header array then balk!
    contains_element $i "${hdr_arr[@]}"
    if [ $? == 1 ]; then
      echo "Key columns not present in header. Abnormal exit."
      exit $E_KEY_ABSENT
    fi
  done
}

check_for_exclude_cols() {
  for i in "${exclude[@]}"; do
    # if the exclude column is not in header array then balk!
    contains_element $i "${hdr_arr[@]}"
    if [ $? == 1 ]; then
      echo "Exclude columns not present in header. Abnormal exit."
      exit $E_EXCLUDE_ABSENT
    fi

    # if the exclude column is in key column array then balk!
    # key columns and exclude columns must be mutually exclusive
    contains_element $i "${key[@]}"
    if [ $? == 0 ]; then
      echo "Exclude columns and key columns overlap. Abnormal exit."
      exit $E_KEY_EXCLUDE_OVERLAP
    fi
  done
}

compare_headers() {
  # Read the header of file2
  hdr=$(sed -n 1p $file2)          # header string
  OLDIFS=$IFS                      # header array
  IFS=","
  i=0
  for h in $hdr; do
    # compare with the corresponding element of header array from file1
    if [ "$h" != "${hdr_arr[$i]}" ]; then
      echo "Headers of the two files do not match. Abnormal exit."
      exit $E_MISMATCHED_HEADERS
    fi
    ((i++))
  done
  # Handle the case where file1 has "C1,C2,C3" and file2 has "C1,C2"
  if [ "$i" -lt "${#hdr_arr[@]}" ]; then
    echo "Headers of the two files do not match. Abnormal exit."
    exit $E_MISMATCHED_HEADERS
  fi
  IFS=$OLDIFS
}

sort_and_diff() {
  # Determine the sort key positions
  j=1
  for i in "${hdr_arr[@]}"; do
    contains_element $i "${key[@]}"
    if [ $? == 0 ]; then
      sortkey="$sortkey -k$j,$j"
    fi
    ((j++))
  done
  # strip the header and sort the files
  sfile1="/tmp/file1.sorted.$$"
  sfile2="/tmp/file2.sorted.$$"
  tfile="/tmp/tmpfile.$$"
  dfile="/tmp/files.diff.$$"

  sed 1d $file1 > $sfile1
  eval "sort $sortkey $sfile1" >$tfile
  mv $tfile $sfile1

  sed 1d $file2 > $sfile2
  eval "sort $sortkey $sfile2" >$tfile
  mv $tfile $sfile2

  # diff them and pass the output to the awk wrapper
  diff $sfile1 $sfile2 >$dfile
  awk -v keys="$keystr" \
      -v files="$filestr" \
      -v tolerance="$tolerance" \
      -v header="$hdr" \
      -v exclude="$excludestr" \
      -f process_diff.awk \
      $dfile

  # cleanup after we are done
  rm $sfile1 $sfile2 $dfile
}

# ==============================================================================
# MAIN SECTION
# ==============================================================================
check_usage $@
process_args $@
read_header
check_for_key_cols
check_for_exclude_cols
compare_headers
sort_and_diff

