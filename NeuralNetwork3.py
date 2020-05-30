import numpy as np
import time
import math
import csv
import sys


class Activation:
    def __init__(self, t):
        self.func = t

    def cal_output(self,mid_res):
        if self.func == "relu":
            return self.relu(mid_res)
        else:
            return self.soft_max(mid_res)

    def soft_max(self, x):
        e_x = np.exp(x - np.max(x))

        return e_x / e_x.sum()
    
    def relu(self, x):

        return np.maximum(0,x)

    def d_relu(self,x):
        x[x <= 0] = 0
        x[x > 0] = 1
        return x


class Layer:
    def __init__(self, num, output_shape, input_shape, active_func):
        self.num = num
        self.output_shape = output_shape
        self.input_shape = input_shape
        self.kernal = np.random.normal(loc = 0, scale = math.sqrt(2/(input_shape+output_shape)), size=(output_shape, input_shape))
        self.bias = np.zeros((output_shape,))
        self.active_func = Activation(active_func)
    
    def cal_res(self, train_x):
        cur_res = np.dot(self.kernal, train_x)
        cur_res = cur_res.T + self.bias
        
        return cur_res.T
    
    def summary_layer(self):
        print("------------------------------------------------------------------------------------")
        print("Layer", self.num, "output_shape:", self.output_shape, "parameter numbers: ", self.input_shape*self.output_shape)
        print("------------------------------------------------------------------------------------")

class NN_Model:
    def __init__(self):
        self.layers = []
        self.mid_res = []
        self.active_res = []
        self.learning_rate = 0.01
        self.beta_1 = 0.9
        self.beta_2 = 0.999
        self.velocity = []
        self.m = []
        self.bias = 1e-8


    def Adam_optimizer(self, layer_num, g, times):
        M = self.m[layer_num]
        V = self.velocity[layer_num]
        M = self.beta_1*M + (1-self.beta_1)*g
        V = self.beta_2*V + (1-self.beta_2)*np.square(g)
        M_norm = M/(1-self.beta_1**times)
        V_norm = V/(1-self.beta_2**times)

        revise_g = M_norm/(np.sqrt(V_norm)+ self.bias)
        self.m[layer_num] = M
        self.velocity[layer_num] = V


        return revise_g

    # train model
    def fit(self, train_x, train_y, branch_size, epochs):
        cur_epoch = 0
        times = 1
        while  cur_epoch <= epochs:
            if cur_epoch ==24 : self.learning_rate /= 10
            epoch_start = time.time()
            for cur_index in range(train_x.shape[0]):
                cur_res = train_x[cur_index].T
                self.active_res.append(cur_res)
                for layer in self.layers:
                    cur_layer = layer
                    cur_res = cur_layer.cal_res(cur_res)
                    self.mid_res.append(cur_res)
                    cur_res = cur_layer.active_func.cal_output(cur_res)
                    self.active_res.append(cur_res)
                cur_res = cur_res.ravel()
            # cross entropy
                # cross_entropy_loss = - train_y[cur_index]* np.log(cur_res)
                loss_array = (train_y[cur_index] - cur_res)
            # backward propagation
                self.back_propagation(loss_array, cur_res,times)
                times += 1
            print("epoch:", cur_epoch, "  time consume: ", round((time.time() - epoch_start)/60, 2))
            cur_epoch += 1
            
        
        return self
    
    #back propagation
    def back_propagation(self, loss_array, result, times):
        d_loss = -loss_array
        previous_d_E = None
        for layer_num in range(len(self.layers)-1, 0, -1) :
            cur_kernal = self.layers[layer_num].kernal
            cur_bias = self.layers[layer_num].bias
            Output = self.active_res[layer_num]
            if layer_num == len(self.layers)-1:
                d = d_loss
            else:
                F_Input_j = self.layers[layer_num].active_func.d_relu(self.mid_res[layer_num])
                d = F_Input_j*previous_d_E

            previous_d_E = np.dot(cur_kernal.T,d)
            cur_bias -= self.learning_rate*d
            g = np.outer(d, Output)
            cur_kernal -= self.learning_rate*g
                    

        
        self.mid_res = []
        self.active_res = []
                

    def predict(self, test_x):
        res = []
        for cur_index in range(test_x.shape[0]):
            cur_res = test_x[cur_index].T
            self.active_res.append(cur_res)
            for layer in self.layers:
                cur_layer = layer
                cur_res = cur_layer.cal_res(cur_res)
                cur_res = cur_layer.active_func.cal_output(cur_res)
            max_category = np.argmax(cur_res)
            res.append(max_category)
        return np.array(res)




    def addLayer(self,layer):
        self.layers.append(layer)
        self.m.append(np.zeros(layer.kernal.shape))
        self.velocity.append(np.zeros(layer.kernal.shape))

            
    def summary_model(self):
        print("Summary of Netural NetWork Model: ")
        for layer in self.layers:
            layer.summary_layer()


def load_data(train_file, train_label, test_file):
    class_num = 10
    train_x = np.loadtxt(train_file, delimiter=",",dtype="float32")/255
    train_y = np.loadtxt(train_label, delimiter=",")
    train_y = Convert_Label_to_Categorical(train_y, class_num)
    test_x = np.loadtxt(test_file, delimiter=",", dtype="float32")/255
    test_y = np.loadtxt("test_label.csv", delimiter=",")

    return train_x, train_y, test_x, test_y

def Convert_Label_to_Categorical(y, class_num):
    label_shape = y.shape[0]
    y = np.array(y, dtype='int').ravel()
    categorical = np.zeros((label_shape, class_num), dtype = 'float32')
    categorical[np.arange(label_shape), y] = 1

    return categorical

def Build_Model():
    model = NN_Model()
    model.addLayer(Layer(1,256, 784, "relu"))
    model.addLayer(Layer(2,128, 256, "relu"))
    model.addLayer(Layer(3,64, 128, "relu"))
    model.addLayer(Layer(4, 32, 64, "relu"))
    model.addLayer(Layer(5, 10, 32, "softmax"))
    return model

def run_model(train_x, train_y, test_x):
    branch_size, epochs = 1, min(50, 1500000/train_x.shape[0])

    model = Build_Model()
    model.summary_model()
    model = model.fit(train_x,train_y, branch_size, epochs)
    predict_y = model.predict(test_x)

    return predict_y


def cal_accuracy(predict_y, test_y):
    label_size = predict_y.shape[0]
    predict_y = np.array(predict_y)
    test_y = np.array(test_y)
    score = sum(predict_y == test_y)
    accuracy = score/label_size
    return accuracy

if __name__ == "__main__":
    
    start_time = time.time()
    train_file = "train_image.csv"
    train_label = "train_label.csv"
    test_file = "test_image.csv"
    if len(sys.argv) == 4:
        train_file = sys.argv[1]
        train_label = sys.argv[2]
        test_file = sys.argv[3]
    train_x, train_y, test_x, test_y = load_data(train_file,train_label, test_file)
    train_x = train_x[:60000]
    predict_y = run_model(train_x, train_y, test_x)
    accuracy = cal_accuracy(predict_y, test_y)
    print("Predict Accuracy: ",accuracy)

    # file = open("test_predictions.csv", "w")
    # writer = csv.writer(file)
    # writer.writerows(predict_y)
    print("Total Duration:", round((time.time()-start_time)/60 ,1))


