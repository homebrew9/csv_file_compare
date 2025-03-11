#!/usr/bin/perl -w
# ==================================================================================================
# Name: compare_files.pl
# Desc: A Perl program to compare csv files in a highly customized way. Floating point numbers can
#       be compared to a specified degree of precision.
# By  : prat
# Revision History:
# By       Date              Remarks
# -------- -----------       ----------------------------------
# prat     10/21/2013        Created
# prat     10/22/2013        Show only mismatched columns, with positions, truncate if p is 0
#                            default p to 0.01 if unspecified
# prat     10/24/2013        Changed the way we compare floats
# prat     10/25/2013        Changed number comparison again, added -x option
# prat     10/30/2013        Made changes to the original program so that it uses less memory
# ==================================================================================================
use strict;
use Getopt::Std;

my %opts;                 # hash for storing arguments
my ($file1, $file2, $dp); # files to compare, degree of precision
my %keys;                 # key columns
my @hdr1;                 # header array for first file
my %dh1;                  # data hashe for first file
my %exclude;              # columns to exclude during comparison; these cannot be key columns!

sub check_usage {
  if ($#ARGV == -1) {
    print "Usage   : perl compare_files.pl -f file1,file2 -k <KEYS> -p <N> -x <EXCLUDE>\n";
    print "            <KEYS>    = comma-delimited list of key columns\n";
    print "            <N>       = degree of precision sought for comparison\n";
    print "            <EXCLUDE> = comma-delimited list of columns to be excluded for comparison\n";
    exit;
  }
}

sub process_args {
  print "Processing args\n";
  getopts("f:k:p:x:", \%opts);
  ($file1, $file2) = split(/,/, $opts{"f"});
  $keys{$_}++ foreach split(/,/, $opts{"k"});
  $dp = $opts{"p"};
  if ( not defined $dp ) {   # if unspecified, $dp defaults to 0.01
    print "Degree of precision not specified, setting it to 0.01\n";
    $dp = 0.01;
  }
  if ( defined $opts{"x"} ) {
    $exclude{$_}++ foreach split(/,/, $opts{"x"});
    # keys cannot be listed as exclude columns; since we **NEED** keys for comparison!
    foreach ( keys %exclude ) {
      if ( defined $keys{$_} ) { print "Exclude column list cannot contain key columns!\n"; exit }
    }
  }
}

sub read_header {
  # We compare headers of both files to:
  # (a) determine if the keys are in the header array
  # (b) check if the header arrays of both files have the same columns, even if
  #     they are in different positions
  # (c) generate the header array data structures for both files
  my ($file, $arr) = @_;
  print "Reading header of file: $file\n";
  open (FH, "<", $file) or die "Can't open $file: $!";
  chomp (my $line = <FH>);
  foreach my $i (split(/,/, $line)) { push @$arr, [ $i ] }
  close (FH) or die "Can't close $file: $!";
}

sub check_for_key_cols {
  my ($k, $h, $f) = @_;
  print "Checking for key columns in file: $f\n";
  my $keycount = 0;
  foreach my $i (@$h) {
    if (defined $k->{$i->[0]} ) { $i->[1] = "K"; $keycount++ }
    else { $i->[1] = "NK" }
  }
  if ($keycount != keys %$k) { print "Key columns not present in file: $f. Abnormal exit!\n"; exit }
}

sub check_for_exclude_cols {
  my ($x, $h, $f) = @_;
  print "Checking for exclude columns in file: $f\n";
  my $xcount = 0;
  foreach my $i (@$h) {
    if (defined $x->{$i->[0]} ) { $i->[2] = "X"; $xcount++ }
    else { $i->[2] = "I" }
  }
  if ($xcount != keys %$x) { print "Exclude columns not present in file: $f. Abnormal exit!\n"; exit }
}

sub compare_file_header {
  # This subroutine compares a specified header array with the first line of the specified file.
  # At this point, the header array should already be finalized by processing one file. We
  # can simply read off the first line of the second file and compare it with the array.
  my ($aref, $file) = @_;
  print "Comparing file header of $file\n";
  open (FH, "<", $file) or die "Can't open $file: $!";
  chomp (my $header = <FH>);
  close (FH) or die "Can't close $file: $!";
  my $i = 0;
  foreach (split(/,/, $header)) {
    if ( $i > $#{$aref} or $_ ne $aref->[$i]->[0] ) { print "Headers of the two files are different! Abnormal exit.\n"; exit; }
    $i++;
  }
  # Capture the exception for missing trailing column(s) in the processed file
  if ( $i  <= $#{$aref} ) { print "Headers of the two files are different! Abnormal exit.\n"; exit; }
}

sub populate_hash {
  # Read the file and populate its data hash. Keys are defined by @hdr arrays and
  # values are arrayrefs with the values of only the Include (I) columns from that line.
  # The key value now has the column position and the I/X status.
  my ($href, $hdr, $file) = @_;
  my $key;                                  # comma delimited values of key columns
  print "Scanning file: $file\n";
  open (FH, "<", $file) or die "Can't open $file: $!";
  while (<FH>) {
    next if $. == 1;                        # we've processed the header already
    chomp (my @line = split(/,/, $_));
    # Inefficient to read the same line twice - first for determining the key
    # columns and then to populate the hashref for its value, but we must know
    # the key of a hash before we set its value.
    # trailing comma found in data file; loop only through header indexes
    for (my $i=0; $i<=$#{$hdr}; $i++) {
      if ( $hdr->[$i]->[1] eq "K" ) { $key .= ",".$line[$i] }
    }
    $key =~ s/^,//;
    for (my $i=0; $i<=$#{$hdr}; $i++) {
      if ( $hdr->[$i]->[1] eq "NK" and $hdr->[$i]->[2] eq "I" ) {
        push ( @{$href->{$key}}, $line[$i] );
        # $href->{$key}->{$hdr->[$i]->[0]} = [ $i, $line[$i], $hdr->[$i]->[2] ] ;
      }
    }
    # flush the key string
    $key = "";
  }
  close (FH) or die "Can't open $file: $!";
}

sub is_number {
  local $_ = shift;
  # code snippet from perlmonks.org, customized for this program
  return 1 if ( /^[+-]?\d+\z/ );                                 # integer comparison
  return 1 if ( /^[+-]?(?=\.?\d)\d*\.?\d*(?:e[+-]?\d+)?\z/i );   # a C float
  return 0;
}

sub is_unequal {
  # This function returns: 1 if the two values are unequal and 0 otherwise
  my ($value1, $value2) = @_;

  # | value1 - value2 | <  dp means values are same for the specified degree of precision
  # | value1 - value2 | >= dp means values are different for the specified degree of precision - prat, 10/24/2013
  if ( is_number($value1) and is_number($value2) ) {    # number comparison
    if ( abs ($value1 - $value2) >= $dp  ) {
      return 1
    }
  } elsif ( $value1 ne $value2 ) {                      # string comparison
    return 1
  }
  return 0;
}

sub compare_file_data {
  # Given the data hash and a file name, loop through the file data and compare the
  # contents with that of the data hash.
  my ($hdr, $href1, $f2, $f1) = @_;
  print "Comparing data in file: $f2\n";
  my $mismatches = "";                             # scalar to store the mismatched column names appended to their positions
  my $key = "";                                    # comma delimited values of key columns
  open (FH, "<", $f2) or die "Can't open $f2: $!";
  while (<FH>) {
    next if $. == 1;       # we have processed the header already
    chomp (my @line = split(/,/, $_));

    # (1) First scan : determine the key
    for (my $i=0; $i<=$#{$hdr}; $i++) {
      if ( $hdr->[$i]->[1] eq "K" ) { $key .= ",".$line[$i] }
    }
    $key =~ s/^,//;

    # (2) If key does not exist in data hash, print message and continue with next line
    # (3) Else loop through the fields and compare values, appending mismatches
    if ( not defined $href1->{$key} ) {
      printf("KEY: %-30s => Found only in %s\n", "($key)", $f2);
      $key = "";
      next;
    } else {
      my $j = 0;
      for (my $i=0; $i<=$#{$hdr}; $i++) {
        if ( $hdr->[$i]->[1] eq "NK" and $hdr->[$i]->[2] eq "I" ) {
           if ( is_unequal ( $href1->{$key}->[$j], $line[$i] ) ) {
             $mismatches .= "," . $hdr->[$i]->[0] . ":". ($i+1);   # show the column position used by "cut" command
           }
           $j++;
        }
      }
      # (4) If mismatches found, then print message
      if ( $mismatches ne "" ) {
        $mismatches =~ s/^,//;
        printf("KEY: %-30s => Mismatched columns = %s\n", "($key)", $mismatches);
        $mismatches = "";
      }
      # (5) Delete the key from the data hash
      delete $href1->{$key};
      $key = "";
    }
  }
  close (FH) or die "Can't close $f2: $!";

  # Loop through the data hash now, print message and delete key
  foreach (keys %$href1) { printf("KEY: %-30s => Found only in %s\n", "($_)", $f1); delete $href1->{$_} }
}

# ==============================================================================
# MAIN SECTION
# ==============================================================================
check_usage;
process_args;
read_header ($file1, \@hdr1);
check_for_key_cols (\%keys, \@hdr1, $file1);
check_for_exclude_cols (\%exclude, \@hdr1, $file1);
compare_file_header (\@hdr1, $file2);
populate_hash (\%dh1, \@hdr1, $file1);
compare_file_data (\@hdr1, \%dh1, $file2, $file1);

