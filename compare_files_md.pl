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
# ==================================================================================================
use strict;
use Getopt::Std;

my %opts;                 # hash for storing arguments
my ($file1, $file2, $dp); # files to compare, degree of precision
my %keys;                 # key columns
my (@hdr1, @hdr2);        # header arrays for each file
my (%dh1, %dh2);          # data hashes for each file
my %exclude;              # columns to exclude during comparison; these cannot be key columns!
my %metadata;             # hash to associate files with their metadata

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
    foreach ( keys %exclude ) { die "Exclude column list cannot contain key columns!" if defined $keys{$_} }
  }
  # Associate file names with their metadata
  %metadata = (
                 $file1 => {
                               "HEADER_ARR" => \@hdr1,
                               "DATA_HASH"  => \%dh1
                           },
                 $file2 => {
                               "HEADER_ARR" => \@hdr2,
                               "DATA_HASH"  => \%dh2
                           }
              );
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
    if (defined $k->{$i->[0]} ) { $i->[1] = "KEY"; $keycount++ }
    else { $i->[1] = "NON_KEY" }
  }
  if ($keycount != keys %$k) { print "Key columns not present in file: $f. Abnormal exit!\n"; exit }
}

sub check_for_exclude_cols {
  my ($x, $h, $f) = @_;
  print "Checking for exclude columns in file: $f\n";
  my $xcount = 0;
  foreach my $i (@$h) {
    if (defined $x->{$i->[0]} ) { $i->[2] = "EXCLUDE"; $xcount++ }
    else { $i->[2] = "INCLUDE" }
  }
  if ($xcount != keys %$x) { print "Exclude columns not present in file: $f. Abnormal exit!\n"; exit }
}

sub compare_headers {
  # This subroutine compares two arrays, though not in the strictest sense.
  # That's because position of elements is **NOT** significant. Thus, the arrays:
  # ('A', 'B', 'C') and ('C', 'A', 'B') are
  # considered equal.
  my ($aref1, $f1, $aref2, $f2) = @_;
  my (@arr1, @arr2);
  print "Comparing headers of files: $f1 and $f2\n";
  foreach my $i (@$aref1) { push (@arr1, $i->[0]) }
  foreach my $i (@$aref2) { push (@arr2, $i->[0]) }
  my $err_msg = sprintf("\t%s: %s\n\t%s: %s\n%s\n", $f1, join(",", @arr1), $f2, join(",", @arr2), "Abnormal exit!");
  # array size and array elements should be the same
  if (@arr1 != @arr2) {
    print "Column counts of the file headers are different.\n";
    print $err_msg;
    exit;
  } elsif ( join(",", sort(@arr1)) ne join(",", sort(@arr2)) ) {
    print "Column names of the file headers are different.\n";
    print $err_msg;
    exit;
  }
}

sub populate_hash {
  # Read the file and populate its data hash. Keys are defined by @hdr arrays and
  # values are hashrefs with each column element as the key and the value as read
  # from that line.
  # The key value now has the column position and the INCLUDE/EXCLUDE status.
  my ($href, $hdr, $file) = @_;
  my @key_arr;                              # array to hold key column values per line
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
      if ( $hdr->[$i]->[1] eq "KEY" ) { push @key_arr, $line[$i] }
    }
    $href->{join(",", @key_arr)}->{"LINE_NUMBER"} = $.;
    for (my $i=0; $i<=$#{$hdr}; $i++) {
      if ( $hdr->[$i]->[1] eq "NON_KEY" ) {
        $href->{join(",", @key_arr)}->{$hdr->[$i]->[0]} = [ $i, $line[$i], $hdr->[$i]->[2] ] ;
      }
    }
    # flush the key array
    @key_arr = ();
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

sub compare_data {
  # This subroutine accepts two values and compares them as integers, reals, floats or strings.
  # In case of floats, the value of $dp is used as the degree of precision for comparison.
  # If the values do not match, then the key  is loaded into the array referenced by $aref.
  my ($key, $pos, $value1, $value2, $aref) = @_;

  # we may need some other strategy if sprintf does not work!
  # sprintf does not work here, we will use the inequality "| value1 - value2 | < dp" to
  # determine if the numbers are the same or different.
  # | value1 - value2 | <  dp means values are same for the specified degree of precision
  # | value1 - value2 | >= dp means values are different for the specified degree of precision - prat, 10/24/2013
  if ( is_number($value1) and is_number($value2) ) {                        # number comparison
    if ( abs ($value1 - $value2) >= $dp  ) { push @$aref, "$key:$pos" }
  } elsif ( $value1 ne $value2 ) {                                          # string comparison
    push @$aref, "$key:$pos";
  }
}

sub compare_hashes {
  # Compare data hashes of both files. A run through hash1 yields a left outer join
  # of hash1 with respect to hash2, and then a run through of hash2 yields the remainder,
  # thereby completing a full outer join.
  my ($href1, $f1, $href2, $f2) = @_;
  print "Comparing data in files: $f1 and $f2\n";
  my @mismatches;                        # array to store the mismatched column names appended to their positions
  while (my ($k, $v) = each %$href1) {
    if ( not defined $href2->{$k} ) {
      printf("KEY: %-30s => Found only in %s:%d\n", "($k)", $f1, $v->{"LINE_NUMBER"});
    } else {
      # key match guaranteed; we now proceed to compare the values, which are hashrefs
      my $hr2 = $href2->{$k};
      while (my ($dk, $dv) = each %$v) {
        next if $dk eq "LINE_NUMBER";      # don't compare line numbers
        next if $dv->[2] eq "EXCLUDE";     # don't compare columns in the exclude list
        compare_data ( $dk, $dv->[0], $dv->[1], $hr2->{$dk}->[1], \@mismatches );
      } # end of while
      if ($#mismatches >= 0) {
        my $mismatched_cols = join(",", @mismatches);
        printf("KEY: %-30s => Mismatched columns = %s\n", "($k)", $mismatched_cols);
      }
      @mismatches = ();
    } # end of else
  } # end of while
  # Complete the full outer join
  while (my ($k, $v) = each %$href2) {
    if ( not defined $href1->{$k} ) {
      printf("KEY: %-30s => Found only in %s:%d\n", "($k)", $f2, $v->{"LINE_NUMBER"});
    }
  }
}

# ==============================================================================
# MAIN SECTION
# ==============================================================================
check_usage;
process_args;
foreach (keys %metadata) { read_header( $_, $metadata{$_}->{"HEADER_ARR"} ) }
foreach (keys %metadata) { check_for_key_cols( \%keys,  $metadata{$_}->{"HEADER_ARR"}, $_ ) }
foreach (keys %metadata) { check_for_exclude_cols( \%exclude,  $metadata{$_}->{"HEADER_ARR"}, $_ ) }
compare_headers ( map { $metadata{$_}->{"HEADER_ARR"}, $_ } (keys %metadata) );
foreach (keys %metadata) { populate_hash( $metadata{$_}->{"DATA_HASH"},  $metadata{$_}->{"HEADER_ARR"}, $_ ) }
compare_hashes ( map { $metadata{$_}->{"DATA_HASH"}, $_ } (keys %metadata) );

