package edu.asu.irs13;

import org.apache.lucene.index.*;
import org.apache.lucene.store.*;

import java.io.File;
import java.util.Scanner;
import java.util.*;

import java.util.ArrayList;

public class SearchFilesIDF {

	public static HashMap<String, Float> idf = new HashMap<String, Float>();

	public static void main(String[] args) throws Exception {
		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
		int maximumDocs = r.maxDoc();
		Date d;
		ArrayList<DocumentSimilarity> restrictedDoc = new ArrayList<DocumentSimilarity>();

		// To compute the 2-Norm of all documents
		double[] normOfDocs = normOfDoc(r);
		Scanner sc = new Scanner(System.in);
		String str = "";
		System.out.print("query> ");
		while (!(str = sc.nextLine()).equals("quit")) {
			d = new Date();
			System.out.println(d.getTime());
			String[] terms = str.split("\\s+");

			DocumentSimilarity[] documentSimilarity = new DocumentSimilarity[maximumDocs];
			for (int i = 0; i < maximumDocs; i++) {
				documentSimilarity[i] = new DocumentSimilarity();
			}

			HashMap<String, Integer> query = freqOfQuery(terms);
			Iterator it = query.entrySet().iterator();
			int value;
			double normOfQuery = 0;
			while (it.hasNext()) {
				Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>) it.next();
				value = (Integer) pair.getValue();
				normOfQuery += value * value;
			}
			// The 2-Norm of the query string
			normOfQuery = Math.sqrt(normOfQuery);

			// Computing the cosine similarity between the document and the
			// query
			for (String word : terms) {
				Term term = new Term("contents", word);
				TermDocs tdocs = r.termDocs(term);
				float idfValue = idf.get(word);
				while (tdocs.next()) {
					documentSimilarity[tdocs.doc()].simillarity += query.get(word) * tdocs.freq() * idfValue;
					documentSimilarity[tdocs.doc()].documentId = tdocs.doc();
				}
			}

			for (int i = 0; i < maximumDocs; i++) {
				if (documentSimilarity[i].simillarity != 0) {
					documentSimilarity[i].simillarity = (documentSimilarity[i].simillarity)
							/ (normOfDocs[i] * normOfQuery);
					restrictedDoc.add(documentSimilarity[i]);
				}
			}

			System.out.println(d.getTime());

			// Ranking the documents
			DocumentSimilarity.quickSort(restrictedDoc, 0, restrictedDoc.size() - 1);
			ArrayList<Integer> clusteringDocuments = new ArrayList<Integer>();
			for (int i = 0; i < 50; i++) {
				clusteringDocuments.add(restrictedDoc.get(i).documentId);
			}

			HashSet<Term> termsInSelectedDocuments = new HashSet<>();
			HashMap<Integer, HashMap<String, Float>> termVectorDocumentRepresentation = new HashMap<>();

			TermEnum termEnum = r.terms();
			Term termsInDoc;
			TermDocs termDocs;
			while (termEnum.next()) {
				termsInDoc = termEnum.term();
				termDocs = r.termDocs(termsInDoc);
				while (termDocs.next()) {
					if (clusteringDocuments.contains(termDocs.doc())) {
						termsInSelectedDocuments.add(termsInDoc);
						HashMap<String, Float> termTFIDF = termVectorDocumentRepresentation.get(termDocs.doc());
						if (termTFIDF == null) {
							termTFIDF = new HashMap<>();
						}

						termTFIDF.put(termsInDoc.text(), (termDocs.freq() * idf.get(termsInDoc.text())));
						termVectorDocumentRepresentation.put(termDocs.doc(), termTFIDF);
					}
				}
			}
			int clusterNumber = 3;
			HashMap<Integer, HashMap<String, Float>> centroidOfClusters = new HashMap<>();
			HashMap<Integer, List<Integer>> clusters = new HashMap<>();
			for (int i = 1; i <= clusterNumber; i++) {
				int startIndex = (i - 1) * 10;
				int stopIndex = (i) * 10;
				if (i == clusterNumber) {
					stopIndex = clusteringDocuments.size();
				}
				List<Integer> newCluster = clusteringDocuments.subList(startIndex, stopIndex);
				clusters.put(i, new ArrayList<Integer>());
				HashMap<String, Float> clusterCentroid = new HashMap<>();
				for (Term term : termsInSelectedDocuments) {
					float centroid = 0;
					for (int doc : newCluster) {
						centroid += (termVectorDocumentRepresentation.get(doc).get(term.text()) == null) ? 0
								: termVectorDocumentRepresentation.get(doc).get(term.text());
					}
					centroid = centroid / newCluster.size();
					clusterCentroid.put(term.text(), centroid);
				}
				centroidOfClusters.put(i, clusterCentroid);
			}

			for (int iteration = 0; iteration < 5; iteration++) {
				for (int doc : clusteringDocuments) {
					int newClusterid = 0;
					float minDistance = 0;
					for (int i = 1; i < clusterNumber; i++) {
						int distanceBetweenClusterAndDocument = 0;
						if (distanceBetweenClusterAndDocument < minDistance) {
							minDistance = distanceBetweenClusterAndDocument;
							newClusterid = i;
						}
					}
					clusters.get(newClusterid).add(doc);
				}
				for (int i = 1; i < clusterNumber; i++) {
					HashMap<String, Float> clusterCentroid = new HashMap<>();
					for (Term term : termsInSelectedDocuments) {
						float centroid = 0;
						List<Integer> newCluster = clusters.get(i);  
						for (int doc : newCluster) {
							centroid += (termVectorDocumentRepresentation.get(doc).get(term.text()) == null) ? 0
									: termVectorDocumentRepresentation.get(doc).get(term.text());
						}
						centroid = centroid / newCluster.size();
						clusterCentroid.put(term.text(), centroid);
					}
					centroidOfClusters.put(i, clusterCentroid);
				}
			}
			
			for(int i = 1; i < clusterNumber; i++ ){
				List<Integer> cluster = clusters.get(i);
				for(int doc : cluster){
					String d_url = r.document(restrictedDoc.get(i).documentId).getFieldable("path").stringValue().replace("%%", "/");
					System.out.println(i +" - ["+restrictedDoc.get(i).documentId+"] " + d_url);
				}
			}

			System.out.print("query> ");

		}
		sc.close();
	}

	private static double[] normOfDoc(IndexReader r) throws Exception {

		double[] normOfDoc = new double[r.maxDoc()];

		TermEnum t = r.terms();
		Term termsInDoc;
		TermDocs termDocs;
		float maxDoc = r.maxDoc();
		float idfVlaue = 0;
		while (t.next()) {
			idfVlaue = maxDoc / t.docFreq();
			idf.put(t.term().text(), (idfVlaue));
			termsInDoc = t.term();
			termDocs = r.termDocs(termsInDoc);
			while (termDocs.next()) {
				normOfDoc[termDocs.doc()] += termDocs.freq() * termDocs.freq();
			}
		}

		for (int i = 0; i < maxDoc; i++) {
			normOfDoc[i] = Math.sqrt(normOfDoc[i]);
		}
		return normOfDoc;

	}

	private static HashMap<String, Integer> freqOfQuery(String[] terms) {
		HashMap<String, Integer> query = new HashMap<String, Integer>();
		Integer j = 0;

		for (String ter : terms) {
			j = query.get(ter);
			if (j == null) {
				query.put(ter, 1);
			} else {
				query.replace(ter, ++j);
			}
		}
		return query;
	}
}