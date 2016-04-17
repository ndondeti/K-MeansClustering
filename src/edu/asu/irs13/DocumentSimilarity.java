package edu.asu.irs13;

import java.util.ArrayList;

public class DocumentSimilarity{
	public double simillarity;
	public int documentId;
	
	public DocumentSimilarity(){
		simillarity = 0;
		documentId = 0;
	}
	
	public DocumentSimilarity(double sim, int id){
		simillarity = sim;
		documentId = id;
	}
	
	private static int partition(ArrayList<DocumentSimilarity> arr, int left, int right)
	{
	      int i = left, j = right;
	      DocumentSimilarity tmp;
	      double pivot = arr.get((left + right) / 2).simillarity;
	     
	      while (i <= j) {
	            while (arr.get(i).simillarity > pivot)
	                  i++;
	            while (arr.get(j).simillarity < pivot)
	                  j--;
	            if (i <= j) {
	                  tmp = arr.get(i);
	                  arr.set(i, arr.get(j));
	                  arr.set(j, tmp) ;
	                  i++;
	                  j--;
	            }
	      };
	     
	      return i;
	}
	 
	public static void quickSort(ArrayList<DocumentSimilarity> arr, int left, int right) {
	      int index = partition(arr, left, right);
	      if (left < index - 1)
	            quickSort(arr, left, index - 1);
	      if (index < right)
	            quickSort(arr, index, right);
	}
}
