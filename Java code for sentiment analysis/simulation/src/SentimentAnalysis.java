/**
 * Sentiment Analysis.
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;


public class SentimentAnalysis 
{
   static ArrayList<String> trainingDocs;
   static ArrayList<String> trainingLabelsList;
   ArrayList<String> stopWordList; 
   static int numClasses;
   static int[] classCounts; //number of docs per class
   static String[] classStrings; //concatenated string for a given class
   static int[] classTokenCounts; //total number of tokens per class
   static HashMap<String,Double>[] condProb;
   static HashSet<String> vocabulary; //entire vocabuary
   Stemmer st = new Stemmer();
   StringBuffer[] sb;
   	
   public SentimentAnalysis(int numC)
   {      
      trainingDocs = new ArrayList<String>();
      trainingLabelsList = new ArrayList<String>();
      stopWordList = new ArrayList<String>();
      numClasses = numC;
      classCounts = new int[numClasses];
      classStrings = new String[numClasses];
      classTokenCounts = new int[numClasses];
      condProb = new HashMap[numClasses];
      vocabulary = new HashSet<String>();
      sb = new StringBuffer[numClasses];
      try
      {
         // Stop words have not been used. file is empty.
         URL url = SentimentAnalysis.class.getClassLoader().getResource("StopWords.txt");
         String filesPathAndName = url.getPath(); 
         Scanner sc2 = new Scanner(new File(filesPathAndName));
         while(sc2.hasNextLine())
         {
            String line = sc2.nextLine();
            String[] stopwords = line.split("\\n");
            for(String stopword : stopwords)
            {
               stopword = stopword.toLowerCase();
               if(!stopWordList.contains(stopword))
                  stopWordList.add(stopword);
            }        
         }
         Collections.sort(stopWordList);
         URL url1 = SentimentAnalysis.class.getClassLoader().getResource("Training_Class.txt");
         String filesPathAndName1 = url1.getPath();
         Scanner sc1 = new Scanner(new File(filesPathAndName1));
         while(sc1.hasNextLine())
         {
            String line = sc1.nextLine();
            String[] trainingLabels = line.split("\\n");
            for(String label : trainingLabels)
               trainingLabelsList.add(label);   
         }
         URL url2 = SentimentAnalysis.class.getClassLoader().getResource("Sentiment_Tweets.txt");
         String filesPathAndName2 = url2.getPath();
         Scanner sc = new Scanner(new File(filesPathAndName2));
         while(sc.hasNextLine())
         {
            String lines = sc.nextLine();
            String[] trainingDocss = lines.split("\\n");
            for(String docss : trainingDocss)
            {
               trainingDocs.add(docss);
            }   
         }
         System.out.println("No of TWEETS(Training Doc Length) : "+trainingDocs.size());
         for(int i=0;i<numClasses;i++)
         {
            classStrings[i] = "";
            sb[i] = new StringBuffer("");
            condProb[i] = new HashMap<String,Double>();
         }
         for(int i=0;i<trainingLabelsList.size();i++)
         {
            classCounts[Integer.parseInt(trainingLabelsList.get(i))]++;            
            //classStrings[Integer.parseInt(trainingLabelsList.get(i))] += (trainingDocs.get(i) + " ");
            sb[Integer.parseInt(trainingLabelsList.get(i))] = sb[Integer.parseInt(trainingLabelsList.get(i))].append((trainingDocs.get(i) + " "));
         }
         for(int h=0;h<numClasses;h++)
         {
            System.out.println("Class "+h+" --"+classCounts[h]);
         }
         for(int i=0;i<numClasses;i++){
            classStrings[i] = sb[i].toString();
            String[] tokens = classStrings[i].split("[\" ()_,?:;%&-]+");
            classTokenCounts[i] = tokens.length;
            System.out.println("No of tokens of class "+i+" : "+tokens.length);
            for(String token:tokens){
               token = token.toLowerCase();
               st.add(token.toCharArray(),token.length());
               st.stem();
               token = st.toString();
               vocabulary.add(token);
               if(condProb[i].containsKey(token))
               {
                  double count = condProb[i].get(token);
                  condProb[i].put(token, count+1);
               }
               else
                  condProb[i].put(token, 1.0);
            }
         }
         for(int i=0;i<numClasses;i++){
            Iterator<Map.Entry<String, Double>> iterator = condProb[i].entrySet().iterator();
            int vSize = vocabulary.size();
            while(iterator.hasNext())
            {
               Map.Entry<String, Double> entry = iterator.next();
               String token = entry.getKey();
               Double count = entry.getValue();
               count = (count+1)/(classTokenCounts[i]+vSize);
               condProb[i].put(token, count);
            }
            System.out.println("Class "+i+" tokens and their conditional probability: \n"+condProb[i]);
         } 
      }
      catch(IOException ioe)
      {
         System.out.println(ioe);
      }
   }
   public int searchStopword(String key)
   {
      int lo = 0;
      int hi = stopWordList.size()-1;
      while(lo<=hi)
      {
         int mid = lo + (hi-lo)/2;
         int result = key.compareTo(stopWordList.get(mid));
         if(result <0) hi = mid - 1;
         else if(result >0) lo = mid+1;
         else 
            return mid;
      }
      return -1;
   }
   public static int classfiy(String doc){
      int label = 0;
      int vSize = vocabulary.size();
      double[] score = new double[numClasses];
      for(int i=0;i<score.length;i++){
         score[i] = Math.log10(classCounts[i]*1.0/trainingDocs.size());
         System.out.println("Class "+i+" score: "+score[i]);
      }
      String[] tokens = doc.split("[\" ()_,?:;%&-]+");
      for(int i=0;i<numClasses;i++){
         for(String token: tokens){
            Stemmer st = new Stemmer();
            st.add(token.toCharArray(),token.length());
            st.stem();
            token = st.toString();
            
            if(condProb[i].containsKey(token))
            {
               score[i] += Math.log10(condProb[i].get(token));
            }
            else
            {
               score[i] += Math.log10(1.0/(classTokenCounts[i]+vSize));
            }
         }
      }
      double maxScore = score[0];
      for(int i=0;i<score.length;i++){
         if(score[i]>maxScore)
         {
            maxScore = score[i];
            label = i;
         }
      }
      return label;
   }
   
   public static void main(String[] args){
      int numClass = 15;
      SentimentAnalysis nb = new SentimentAnalysis(numClass);
      try
      {
    	 URL url = SentimentAnalysis.class.getClassLoader().getResource("Sentimental_Input.txt");
         String filesPathAndName = url.getPath();
         BufferedReader sc2 = new BufferedReader(new FileReader(filesPathAndName));
         FileWriter fw = new FileWriter("C:\\Users\\Harsha\\Desktop\\Sentimental_Output.txt");
         BufferedWriter bf = new BufferedWriter(fw);
         int i=1;
         String line;
         bf.write("\nLine no"+i+"-----Input text------"+"----Sentiment Score----");
         while((line=sc2.readLine())!=null)
         {
            int sentiment = (nb.classfiy(line));
            bf.write("\n"+i+","+line+","+sentiment);
            i++;
         }
         bf.flush();
         bf.close();
         fw.close();
         sc2.close();
         System.out.println("*********************");
      }
      catch(Exception e)
      {
         System.out.println(e.getMessage());
      }
   }
}