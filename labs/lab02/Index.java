import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

/**
 * Index Generator for Hadoop Map Reduce.
 * 
 * Adapted from the {@link http://wiki.apache.org/hadoop/WordCount Hadoop wiki}.
 */
public class Index {

    /** Mapper for Index.
     *
     * The base class Mapper is parameterized by
     * <in key type, in value type, out key type, out value type>.
     *
     * Thus, this mapper takes (Text key, Text value) pairs and outputs
     * (Text key, LongWritable value) pairs. The input keys are assumed
     * to be identifiers for documents, which are ignored, and the values
     * to be the content of documents. The output keys are words found
     * within each document, and the output values are the number of times
     * a word appeared within a document.
     *
     * To support efficient serialization (conversion of data to and from
     * formats suitable for transport), Hadoop typically does not use the
     * built-in Java classes like "String" and "Long" as key or value types. The
     * wrappers Text and LongWritable implement Hadoop's serialization
     * interface (called Writable) and, unlike Java's String and Long, are
     * mutable.
     */
    public static class IndexMap extends Mapper<Text, Text, Text, LongWritable> {
        /** Regex pattern to find words (alphanumeric + _). */
        final static Pattern WORD_PATTERN = Pattern.compile("\\w+");

        /** Actual map function. Takes one document's text and emits key-value
         * pairs for each word found in the document.
         *
         * @param key Document identifier (ignored).
         * @param value Text of the current document.
         * @param context MapperContext object for accessing output, 
         *                configuration information, etc.
         */
        @Override
        public void map(Text key, Text value, Context context)
                throws IOException, InterruptedException {

            /*--- YOUR CODE HERE---*/
            long index = 0;
            Matcher matcher = WORD_PATTERN.matcher(value.toString());
            while (matcher.find()) {
                String word = matcher.group();
                context.write(new Text(word.toString()+" "+key.toString()+":"), new LongWritable(index));
                index++;
            }
        }
    }

    /** Reducer for Index. 
	 * Currently it has the same type signature as WordCount, which you will
	 * need to modify.
     *
     * Like the Mapper base class, the base class Reducer is parameterized by 
     * <in key type, in value type, out key type, out value type>.
     */
    public static class IndexReduce extends Reducer<Text, LongWritable, Text, Text> {
        /** Actual reduce function.
         * 
         * @param key Word.
         * @param values Values for this word (partial counts).
         * @param context ReducerContext object for accessing output,
         *                configuration information, etc.
         */
        @Override 
        public void reduce(Text key, Iterable<LongWritable> values,
                Context context) throws IOException, InterruptedException {


            /* ---YOUR CODE HERE--- */
                String list = "";
                for(LongWritable value : values){
                        list += value.get()+", ";
                }

                context.write(key, new Text(list.substring(0, list.length()-2))); 



        }
    }

    /** Entry-point for our program. Constructs a Job object representing a single
     * Map-Reduce job and asks Hadoop to run it. When running on a cluster, the
     * final "waitForCompletion" call will distribute the code for this job across
     * the cluster.
     *
     * @param rawArgs command-line arguments
     */
    public static void main(String[] rawArgs) throws Exception {
        /* Use Hadoop's GenericOptionsParser, so our MapReduce program can accept
         * common Hadoop options.
         */
        GenericOptionsParser parser = new GenericOptionsParser(rawArgs);
        Configuration conf = parser.getConfiguration();
        String[] args = parser.getRemainingArgs();

        /* Create an object to represent a Job. */
        Job job = new Job(conf, "wordcount");

        /* Tell Hadoop where to locate the code that must be shipped if this
         * job is to be run across a cluster. Unless the location of code
         * is specified in some other way (e.g. the -libjars command line
         * option), all non-Hadoop code required to run this job must be
         * contained in the JAR containing the specified class (IndexMap 
         * in this case).
         */
        job.setJarByClass(IndexMap.class);

        /* Set the datatypes of the keys and values outputted by the maps and reduces.
         * These must agree with the types used by the Mapper and Reducer. Mismatches
         * will not be caught until runtime.
         */
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(LongWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        /* Set the mapper, combiner, reducer to use. These reference the classes defined above. */
        job.setMapperClass(IndexMap.class);
        job.setReducerClass(IndexReduce.class);
    
        /* Set the format to expect input in and write output in. The input files we have
         * provided are in Hadoop's "sequence file" format, which allows for keys and
         * values of arbitrary Hadoop-supported types and supports compression.
         *
         * The output format TextOutputFormat outputs each key-value pair as a line
         * "key<tab>value".
         */
        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        /* Specify the input and output locations to use for this job. */
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        /* Submit the job and wait for it to finish. The argument specifies whether
         * to print progress information to output. (true means to do so.)
         */
        job.waitForCompletion(true);
    }

}
