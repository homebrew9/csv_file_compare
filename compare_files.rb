#!/usr/bin/ruby
# ==============================================================================
# Name: compare_files.rb
# Desc: A Ruby program to compare files.
# By:   prat
# On:   11/21/2013
# ==============================================================================
require 'optparse'

E_INVALID_ARGS = 1
E_INVALID_FILENAME = 2
E_INVALID_KEY = 3
E_INVALID_PRECISION = 4
E_INVALID_EXCLUDE = 5
E_KEY_EXCLUDE_OVERLAP = 6
E_INCORRECT_FILE_COUNT = 7
E_KEY_NOT_FOUND = 8
E_EXCLUDE_NOT_FOUND = 9
E_UNEQUAL_HEADERS = 10
$dp = 0.01
$keys = Hash.new
$exclude = Hash.new
$hdr = Array.new
$data = Hash.new

# ==============================================================================
# CLASS DEFINITIONS
# ==============================================================================
class Header
  def initialize(name)
    @name = name
    @iskey = false
    @isexclude = false
  end
  def name; @name; end
  def iskey; @iskey; end
  def isexclude; @isexclude; end
  def iskey=(value)
    @iskey = value
  end
  def isexclude=(value)
    @isexclude = value
  end
  def to_s                           # Return a String that represents this header
    "(#@name, #@iskey, #@isexclude)" # Just interpolate the instance variables into a string
  end
end

# ==============================================================================
# GLOBAL FUNCTIONS
# ==============================================================================
def is_number?(object)
  true if Float(object) rescue false
end

def process_args
  OptionParser.new do |o|
    o.on("-f", "--files FILE_LIST", "Comma-delimited list of files to compare") do |files|
      $files = files.split(",")
      $files.each do |x|
        if x =~ /^-/ or x == ""
          print "Invalid file name!\n"
          exit E_INVALID_FILENAME
        end
      end
    end
    o.on("-k", "--keys KEY_LIST", "Comma-delimited list of keys") do |keylist|
      keylist.split(",").each do |k|
        if k =~ /^-/ or k == ""
          print "Invalid key column!\n"
          exit E_INVALID_KEY
        end
        $keys[k] = 0
      end
    end
    o.on("-p", "--precision N", "Degree of precision") do |n|
      $dp = n
      if not is_number?($dp)
        print "Invalid degree of precision!\n"
        exit E_INVALID_PRECISION
      end
    end
    o.on("-x", "--exclude EXCLUDE_LIST", "Comma-delimited list of columns to be excluded from comparison") do |xlist|
      xlist.split(",").each do |x|
        if x =~ /^-/ or x == ""
          print "Invalid exclude column!\n"
          exit E_INVALID_EXCLUDE
        end
        $exclude[x] = 0
      end
    end
    o.on("-h", "--help", "Show this message") do
      puts o
      exit E_INVALID_ARGS
    end
    if ARGV.length == 0
      puts o
      exit E_INVALID_ARGS
    end
    o.parse!
  end
  # Balk if key and exclude columns overlap
  $keys.each_key do |x|
    if $exclude.has_key?(x)
      print "Keys and exclude columns overlap!\n"
      exit E_KEY_EXCLUDE_OVERLAP
    end
  end
  # Or if number of files is not 2
  if $files.size != 2
    print "Incorrect number of files specified!\n"
    exit E_INCORRECT_FILE_COUNT
  end
end

def read_header(file)
  File.open(file) do |f|
    f.readline.chomp!.split(",").each do |x|
      a = Header.new(x)
      if $keys.has_key?(x)
        a.iskey = true
        $keys[x] = 1
      end
      if $exclude.has_key?(x)
        a.isexclude = true
        $exclude[x] = 1
      end
      # puts a
      $hdr << a
    end
  end
  # All keys and exclude cols must be present in the header
  $keys.each do |k, v|
    if v == 0
      print "Key ", k, " was not found in the header!\n"
      exit E_KEY_NOT_FOUND
    end
  end
  $exclude.each do |k, v|
    if v == 0
      print "Exclude column ", k, " was not found in the header!\n"
      exit E_EXCLUDE_NOT_FOUND
    end
  end
end

def compare_headers(file)
  arr1 = Array.new
  arr2 = Array.new
  $hdr.each {|x| arr1 << x.name}
  File.open(file) {|f| arr2 = f.readline.chomp!.split(",")}
  if (arr1 <=> arr2) != 0
    print "The headers of the two files do not match!\n"
    exit E_UNEQUAL_HEADERS
  end
end

def read_data(file)
  arr = Array.new
  key = ""
  fh = File.open(file)
  fh.each do |line|
    next if $. == 1
    line.chomp!
    line.split(",").each_with_index do |token, pos|
      next if pos >= $hdr.size
      if $hdr[pos].iskey
        key << "," + token
        arr << ""
      elsif $hdr[pos].isexclude
        arr << ""
      else
        arr << token
      end
    end
    key[0] = ""
    $data[key] = arr
    key, arr = "", []
  end
end

def compare_data(files)
  arr = Array.new
  key = ""
  iter, ref = files    # iter = file to iterate through; ref = reference file
  fh = File.open(iter)
  fh.each do |line|
    next if $. == 1
    line.chomp!
    line.split(",").each_with_index do |token, pos|
      next if pos >= $hdr.size
      if $hdr[pos].iskey
        key << "," + token
        arr << ""
      elsif $hdr[pos].isexclude
        arr << ""
      else
        arr << token
      end
    end
    key[0] = ""
    # if key exists in data hash then compare non-key values,
    # and print message only if any non-key columns are different
    # if key does not exist in data hash then print message
    if $data.has_key?(key)
      mismatches = compare_arrays(arr, $data[key])
      if mismatches != ""
        printf("KEY: %-30s => Mismatched columns = %s\n", "(" + key + ")", mismatches)
      end
      $data.delete(key)
    else
      printf("KEY: %-30s => Found only in %s\n", "(" + key + ")", iter)
    end
    key, arr = "", []
  end
  # the remaining keys in data hash were found only in the ref file
  $data.keys.each do |k|
    printf("KEY: %-30s => Found only in %s\n", "(" + k + ")", ref)
  end
end

def compare_arrays(arr1, arr2)
  str = ""
  arr1.each_with_index do |elem, i|
    if is_number?(elem) and is_number?(arr2[i])
      str << "," + $hdr[i].name + ":" + (i+1).to_s if (Float(elem) - Float(arr2[i])).abs >= Float($dp)
    else
      str << "," + $hdr[i].name + ":" + (i+1).to_s if elem != arr2[i]
    end
  end
  str[0] = ""
  return str
end

# ==============================================================================
# MAIN SECTION
# ==============================================================================
process_args
read_header($files[0])
compare_headers($files[1])
read_data($files[0])
compare_data($files.values_at(1,0))

