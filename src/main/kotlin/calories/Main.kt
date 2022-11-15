package calories

import krangl.DataFrame
import krangl.print
import krangl.readCSV
import org.nield.kotlinstatistics.NaiveBayesClassifier
import org.nield.kotlinstatistics.toNaiveBayesClassifier

private val df = DataFrame.readCSV("src/main/resources/calories.csv")
private var epoch = 0
private lateinit var nbc: NaiveBayesClassifier<Set<String>, Int>
private var k1 = 0.5
private var k2 = 2 * k1
private var predictionResult = Triple(0, 0.0, 0)

private fun checkDataFrame() {
	df.print()
}

private fun mapTransform() =
	df.rows.map {
		Model(
			it["food"] as String,
			it["category"] as String,
			it["calories"] as Int,
		)
	}

private fun train(k1: Double = 0.5, k2: Double = 1.0) = run {
	val data = mapTransform()
	
	nbc = data.toNaiveBayesClassifier(
		featuresSelector = { listOf(
			it.food.splitWords().toSet(),
			it.category.splitWords().toSet())
		},
		categorySelector = { it.calories },
		k1 = k1,
		k2 = k2
	)
	
	epoch++
	nbc
}

private fun predict() {
	train(k1, k2)
	k1 -= 0.001
	k2 = 2 * k1
	
	val prediction = nbc.predictWithProbability("Sate Ayam".splitWords().toSet(), "Dinner".splitWords().toSet())
	val result = prediction?.category ?: -1
	val accuracy = prediction?.probability ?: -1.0
	
	println("Epoch: $epoch, Result: $result, Accuracy: $accuracy")
	predictionResult = if (accuracy >= 0.7) {
		Triple(result, accuracy, epoch)
	} else {
		Triple(-1, -1.0, epoch)
	}
	
	accuracy.epochCallback {
		if(k1 > 0 && k2 > 0) {
			predict()
		}
	}
}

private fun Double.epochCallback(block: () -> Unit) = run {
	if (this != -1.0 && this < 0.93 && epoch < 1000) {
		block()
	}
}

fun main() {
	checkDataFrame()
	predict()
	
	println("Final Result -> Epoch: ${predictionResult.third}, Result: ${predictionResult.first}, Accuracy: ${predictionResult.second}")
}

fun String.splitWords() =  split(Regex("\\s")).asSequence()
	.map { it.replace(Regex("[^A-Za-z]"),"").toLowerCase() }
	.filter { it.isNotEmpty() }