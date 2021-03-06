(ns jutsu.ai.test
  (:require [clojure.test :refer :all]
            [jutsu.ai.core :as ai]
            [jutsu.matrix.core :as m]))

(def n (ai/network [:optimization-algo :sgd 
                           :learning-rate 0.5
                           :momentum 0.9
                           :layers [[:dense  [:n-in 1 :n-out 2 :activation :tanh]]
                                    [:dense  [:n-in 2 :n-out 2 :activation :tanh]]
                                    [:output :mse [:n-in 2 :n-out 1 
                                                   :activation :identity]]]  
                           :pretrain false
                           :backprop true]))

(def layer-config-test-2 [:optimization-algo :sgd 
                          :learning-rate 0.5
                          :momentum 0.9
                          :layers [[:dense [:n-in 4 :n-out 4 :activation :relu]]
                                   [:dense [:n-in 4 :n-out 4 :activation :relu]]
                                   [:output :negative-log-likelihood [:n-in 4 :n-out 10 
                                                                      :activation :softmax]]]
                          :pretrain false
                          :backprop true])
                          
(def test-net (ai/network layer-config-test-2))

(deftest init-classification-net
  (is (= org.deeplearning4j.nn.multilayer.MultiLayerNetwork (class test-net))))

(defn train-classification-net []
  (let [dataset-iterator (ai/classification-csv-iterator "classification_data.csv" 4 4 10)
        network (ai/initialize-net test-net)]
    (-> network
        (ai/train-net! 10 dataset-iterator)
        (ai/save-model "classnet.zip"))
    (.reset dataset-iterator)
    (println (ai/evaluate network dataset-iterator))))

(train-classification-net)

(def autoencoder-config2
  [:layers [[:rbm [:n-in 2000 :n-out 1000 :loss-function :kl-divergence]]
            [:rbm [:n-in 1000 :n-out 500 :loss-function :kl-divergence]]
            [:rbm [:n-in 500 :n-out 250 :loss-function :kl-divergence]]
            [:rbm [:n-in 250 :n-out 100 :loss-function :kl-divergence]]
            [:rbm [:n-in 100 :n-out 30 :loss-function :kl-divergence]]
            [:rbm [:n-in 30 :n-out 100 :loss-function :kl-divergence]]
            [:rbm [:n-in 100 :n-out 250 :loss-function :kl-divergence]]
            [:rbm [:n-in 250 :n-out 500 :loss-function :kl-divergence]]
            [:rbm [:n-in 500 :n-out 1000 :loss-function :kl-divergence]]
            [:output :mse [:n-in 1000 :n-out 2000 :activation :sigmoid]]]])

(def test-encoder (ai/network autoencoder-config2))

(deftest init-autoencoder
  (is (= org.deeplearning4j.nn.multilayer.MultiLayerNetwork (class test-encoder))))

(def iris-net-config2 [:layers [[:dense [:n-in 4 :n-out 10 :activation :relu]]
                                [:dense [:n-in 10 :n-out 10 :activation :relu]]
                                [:output :negative-log-likelihood [:n-in 10 :n-out 3
                                                                   :activation :softmax]]]])

(def iris-net (ai/network iris-net-config2))

(defn iris-train []
  (let [iris-iterator (ai/classification-csv-iterator "iris.csv" 150 4 3)
        network (ai/initialize-net iris-net)]
    (-> network
        (ai/train-net! 200 iris-iterator)
        (ai/save-model "iris-model"))
    (.reset iris-iterator)
    (println (ai/evaluate network iris-iterator))
    (->> (m/matrix [7.1 3.0 5.9 2.1])
     (ai/output network)
     (m/max-index)
     (println))))

(iris-train)

(def rnn-config2
  [:layers [[:dense [:n-in 1 :n-out 10 :activation :tanh]]
            [:rnn-output :mse [:n-out 10 :n-in 1 :activation :identity]]]])

(def test-rnn (ai/network rnn-config2))

(deftest init-rnn
  (is (= org.deeplearning4j.nn.multilayer.MultiLayerNetwork (class test-rnn))))

;;From https://github.com/deeplearning4j/dl4j-examples/blob/master/dl4j-examples/src/main/java/org/deeplearning4j/examples/convolution/LenetMnistExample.java
(def cnn-config
  [:seed 123
   :iterations 1
   :regularization true
   :l2 0.0005
   :learning-rate 0.01
   :learning-rate-decay-policy :learning-rate-policy-schedule
   :learning-rate-schedule {0 0.01 1000 0.005 3000 0.001}
   :weight-init :xavier
   :optimization-algo :sgd
   :updater :nesterovs
   :layers [[:convolution [5 5] [:n-in 1 :stride [1 1] :n-out 20 :activation :identity]]
            [:sub-sampling :pooling-type-max [:kernel-size [2 2] :stride [2 2]]]
            [:convolution [5 5] [:stride [1 1] :n-out 50 :activation :identity]]
            [:sub-sampling :pooling-type-max [:kernel-size [2 2] :stride [2 2]]]
            [:dense [:activation :relu :n-out 500]]
            [:output :negative-log-likelihood [:n-out 20 :activation :softmax]]]
   :set-input-type (ai/input-type-convolutional-flat 28 28 1)
   :backprop true
   :pretrain false])

(def test-cnn (ai/network cnn-config))

(deftest init-cnn
  (is (= org.deeplearning4j.nn.multilayer.MultiLayerNetwork (class test-cnn))))

;;https://github.com/deeplearning4j/dl4j-examples/blob/master/dl4j-examples/src/main/java/org/deeplearning4j/examples/convolution/AnimalsClassification.java
;;.layer(10, fullyConnected("ffn1", 4096, nonZeroBias, dropOut, new GaussianDistribution(0, 0.005)))
(def animals-cnn-config
  [:seed 123
   :weight-init :distribution
   :dist (ai/normal-distribution 0.0 0.1)
   :activation :relu
   :updater :nesterovs
   :iterations 1
   :gradient-normalization :renormalize-l2-per-layer
   :optimization-algo :sgd
   :learning-rate 1e-2
   :bias-learning-rate (* 1e-2 2)
   :learning-rate-decay-policy :step
   :lr-policy-decay-rate 0.1
   :lr-policy-steps 100000
   :regularization true
   :l2 (* 5 1e-4)
   :mini-batch false
   :layers [[:convolution [11 11] [4 4] [3 3] [:name "cnn1" :n-in 3 :n-out 96 :bias-init 0]]
            [:local-response-normalization [:name "lrn1"]]
            [:sub-sampling [3 3] [2 2] [:name "maxpool1"]]
            [:convolution [5 5] [1 1] [2 2] [:name "cnn2" :n-out 256 :bias-init 1]]
            [:local-response-normalization [:name "lrn2"]]
            [:sub-sampling [3 3] [2 2] [:name "maxpool2"]]
            [:convolution [3 3] [1 1] [1 1] [:name "cnn3" :n-out 384 :bias-init 0]]
            [:convolution [3 3] [1 1] [1 1] [:name "cnn4" :n-out 384 :bias-init 1]]
            [:convolution [3 3] [1 1] [1 1] [:name "cnn5" :n-out 256 :bias-init 1]]
            [:dense [:name "ffn1" :n-out 4096 :bias-init 1 :drop-out 0.5 :dist (ai/guassian-distribution 0 0.005)]]
            [:dense [:name "ffn2" :n-out 4096 :bias-init 1 :drop-out 0.5 :dist (ai/guassian-distribution 0 0.005)]]
            [:output :negative-log-likelihood [:name "output" :n-out 4 :activation :softmax]]]
   :backprop true
   :pretrain false
   :set-input-type (ai/input-type-convolutional 100 100 3)])

(def animals-cnn (ai/network animals-cnn-config))

(deftest init-cnn
  (is (= org.deeplearning4j.nn.multilayer.MultiLayerNetwork (class animals-cnn))))
