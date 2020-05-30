# 4287866445  Tingyi Guo
import sys
import numpy as np
import random
import time
import collections
from Basic_Function import Board, Reader, Writer

class MyPlayer():
    def __init__(self):
        self.type = 'my'

    def random_walk(self, go, piece_type):
        possible_placements = []
        for i in range(5):
            for j in range(5):
                if go.valid_place_check(i, j, piece_type):
                    possible_placements.append((i, j))

        if len(possible_placements) == 0:
            return "PASS"
        else:
            return random.choice(possible_placements)
    
    # try to occupy four coner of the board [(1,1), (1,3), (3,1), (3,3)] and center of board
    def valid_key_place(self, go, piece_type):
        count = 0
        for place in [(2,2), (1,1), (3,3), (1,3), (3,1)]:
            if go.valid_place_check(place[0], place[1], piece_type):
                count += 1
        return count

    def try_hold_conrner(self, go, piece_type):
        solution = []
        #if go.valid_place_check(2, 2, piece_type) and piece_type == 1: return (2, 2)
        for place in [(2,2), (1,1), (3,3), (1,3), (3,1)]:
            if go.valid_place_check(place[0], place[1], piece_type):
                cur_neighbor = go.detect_around(place[0], place[1])
                for neighbor_place in cur_neighbor:
                    if go.board[neighbor_place[0]][neighbor_place[1]] == 3 - piece_type:
                        return place
                solution.append(place)
        if len(solution) == 0:
            return None
        if (2,2) in solution:
            return (2,2)
        return random.choice(solution)
    
    # try to protect myself
    def try_protect_myslef(self, go, piece_type):
        next_piece = 3 - piece_type
        max_danger = 0
        protect_place = list()
        for i in range(5):
            for j in range(5):
                if go.valid_place_check(i,j,piece_type):
                    if go.valid_place_check(i,j, next_piece) :
                        next_go = go.copy_board()
                        next_go.place_chess(i, j, next_piece)
                        remove_count = len(go.find_died_pieces(piece_type))
                        if remove_count > max_danger:
                            protect_place = [(i,j)]
                            max_danger = remove_count
                        if remove_count == max_danger:
                            protect_place.append((i,j))
        if max_danger == 0 or len(protect_place) == 0:
            return None
        return protect_place
                
    # try to eat pieces
    def try_attack(self, go, piece_type):#greedy
        max_remove = 0
        best_place = list()
        for i in range(5):
            for j in range(5):
                if go.valid_place_check(i, j, piece_type) :
                    next_board = go.copy_board()
                    next_board.place_chess(i, j, piece_type)
                    remove_count = len(next_board.find_died_pieces(3-piece_type))
                    if remove_count >max_remove:
                        max_remove = remove_count
                        best_place = [(i,j)]
                    if remove_count == max_remove:
                        best_place.append((i,j))
        if len(best_place) == 0 or max_remove == 0:
            return None
        return best_place
    
    # make a trap to make one place become unvalid
    def try_make_trap(self, go, piece_type):
        trap_action = list()
        for i in range(5):
            for j in range(5):
                if go.valid_place_check(i, j, piece_type) :
                    next_go = go.copy_board()
                    next_go.place_chess(i, j, piece_type)
                    for x, y in next_go.detect_neighbor(i, j):
                        if x in range(5) and y in range(5) and not next_go.valid_place_check(x, y, 3-piece_type):
                            if next_go.board[x][y] == 0:
                                trap_action.append((i,j))
        if len(trap_action) != 0:
            return trap_action
        return None
    
    # put my piece near to oppoent pieces, make oppoent has fewer ally.
    def try_launch_offense(self, go, piece_type):
        ally_dict = collections.defaultdict(int)
        open_dict = collections.defaultdict(list)
        cur_board = go.board
        for x in range(5):
            for y in range(5):
                if cur_board[x][y] == 3 - piece_type:
                    cur_neighbor = go.detect_neighbor(x, y)
                    for place in cur_neighbor:
                        if cur_board[place[0]][place[1]] == piece_type:
                            ally_dict[(x,y)] += 1
                        if cur_board[place[0]][place[1]] == 0 and go.valid_place_check(place[0], place[1], piece_type):
                            open_dict[(x,y)].appned((place[0], place[1]))
        res = list()
        for key, value in ally_dict.items():
            if value == max(ally_dict.values()) and key in open_dict.keys():
                res += open_dict[key]
        if len(res) == 0:
            return None
        return random.choice(res)

    
    # run minmax
    def run_MIN_MAX(self, go, piece_type, max_deep):
        possible_place = go.benefits_place(piece_type)
        a, b = (float("-inf"),float("-inf"),float("-inf")), (float("inf"),float("inf"),float("inf"))
        value, position = self.find_max_place(go, piece_type, possible_place, a, b, max_deep)
        return value, position

    def cal_score_white(self, go, piece_type):
        score, liberty_diff, liberty = 0, 0, 0 
        board = go.board
        for x in range(5):
            for y in range(5):
                if board[x][y] == piece_type:
                    score += 1
                    cur_neighbor = go.detect_neighbor(x,y)
                    for place in cur_neighbor:
                        if board[place[0]][place[1]] == 0:
                            liberty_diff += 1
                            liberty += 1
                if board[x][y] == 3 - piece_type:
                    score -= 1
                    cur_neighbor = go.detect_neighbor(x,y)
                    for place in cur_neighbor:
                        if board[place[0]][place[1]] == 0:
                            liberty_diff -= 1    
                    
        return (score, liberty_diff, liberty)
    
    def cal_score_black(self, go, piece_type):
        score, possible_eat_place,liberty_black, liberty_white = 0, 0, 0, 0
        board = go.board
        board_black = go.board
        board_white = go.board
        for x in range(5):
            for y in range(5):
                if board[x][y] == piece_type:
                    score += 1
                    cur_neighbor = go.detect_neighbor(x,y)
                    for place in cur_neighbor:
                        if board_black[place[0]][place[1]] == 0:
                            liberty_black += 1
                            board_black[place[0]][place[1]] = -1
                if board[x][y] == 3 - piece_type:
                    score -= 1
                    cur_neighbor = go.detect_neighbor(x,y)
                    for place in cur_neighbor:
                        if board_white[place[0]][place[1]] == 0:
                            board_white[place[0]][place[1]] = -1
                            liberty_white += 1
                if board[x][y] == 0 and go.valid_place_check(x, y, 3-piece_type):
                    next_board = go.copy_board()
                    next_board.place_chess(x, y, 3-piece_type)
                    if len(next_board.find_died_pieces(piece_type)) > 0:
                        possible_eat_place += 1
                    
        return (score, possible_eat_place, liberty_black, -liberty_white)
        

    def find_max_place(self, go, piece_type, possible_place, a, b, deep):
        if go.move_num == 25 or len(possible_place) == 0 or deep == 0:
            if piece_type == 1: value = self.cal_score_black(go, piece_type)
            else: value = self.cal_score_white(go, piece_type)
            return value, None
        cur_max = (float("-inf"),float("-inf"),float("-inf"))
        cur_place = None
        for place in possible_place:
            next_board = go.copy_board()
            next_board.place_chess(place[0], place[1], piece_type)
            next_board.died_pieces = next_board.remove_died_pieces(3 - piece_type)
            next_board.move_num = go.move_num + 1
            children_possible_place = next_board.benefits_place(3 - piece_type)
            next_value, next_position = self.find_min_place(next_board, 3-piece_type, children_possible_place, a, b, deep - 1)
            if cur_max < next_value:
                cur_place = place
                cur_max = next_value
            if cur_max >= b:
                return cur_max, cur_place
            a = max(a, cur_max)
        
        return cur_max, cur_place


    def find_min_place(self, go, piece_type, possible_place, a, b, deep):
        if go.move_num == 25 or len(possible_place) == 0 or deep == 0:
            if piece_type == 2: value = self.cal_score_black(go, 3-piece_type)
            else: value = self.cal_score_white(go, 3-piece_type)
            return value, None
        cur_min = (float("inf"),float("inf"),float("inf"))
        cur_place = None
        for place in possible_place:
            next_board = go.copy_board()
            next_board.place_chess(place[0], place[1], piece_type)
            next_board.died_pieces = next_board.remove_died_pieces(3 - piece_type)
            next_board.move_num = go.move_num + 1
            children_possible_place = next_board.benefits_place(3 - piece_type)
            next_value, next_position = self.find_max_place(next_board, 3-piece_type, children_possible_place, a, b, deep - 1)
            if cur_min > next_value:
                cur_place = place
                cur_min = next_value
            if cur_min <= a:
                return cur_min, cur_place
            b = min(b, cur_min)
        
        return cur_min, cur_place

            

    def get_input(self, go, piece_type):
        # At the begining of a game, we try to occupy the key place of the board
        if self.valid_key_place(go, piece_type) > 0:
            #try to occcupy conrner
            occupy_solution = self.try_hold_conrner(go, piece_type)
            if occupy_solution != None:
                print('occupy')
                return occupy_solution
            return self.random_walk(go, piece_type)
            
                                
        # if the key places have been occupied, we run minmax to predict the most benefits place.
        else:
            #try minmax
            if len(go.benefits_place(piece_type))<=5: max_deep = 6
            if len(go.benefits_place(piece_type))>=10: max_deep = 3
            else: max_deep = 4
            solution, place = self.run_MIN_MAX(go, piece_type, max_deep)
            # # if we do not get one useful solution from minmax, we use some methods to decide the place.
            # if place == None or not go.valid_place_check(place[0], place[1], piece_type):
            #     print('use strategies')
            #     # try to attack
            #     attack_solution = self.try_attack(go, piece_type)
            #     if attack_solution != None:
            #         print('attack')
            #         return random.choice(attack_solution)
            # # No attack solution, try to protect myself
            #     protect_solution = self.try_protect_myslef(go, piece_type)
            #     if protect_solution != None:
            #         print('defense')
            #         return random.choice(protect_solution)
            # # Don't need to protect, make a trap
            #     trap_solution = self.try_make_trap(go, piece_type)
            #     if trap_solution != None:
            #         print('trap')
            #     return random.choice(trap_solution)
            
            # # try to launch offense, decrease the number of open for oppent piece
            #     launch_solution = self.try_launch_offense(go, piece_type)
            #     if launch_solution != None:
            #         return launch_solution
            #     return self.random_walk(go, piece_type)
            return place



if __name__ == "__main__":
    s = time.time()
    reader, writer = Reader(), Writer()
    piece_type, previous_board, board =reader.readInputFile()
    if piece_type == 1:
        file = open("move_count_black.txt", 'r')
    else:
        file = open("move_count_white.txt", 'r')
    move = int(file.readline())
    go = Board()
    go.set_board(piece_type, previous_board, board)
    go.move_num = move
    player = MyPlayer()
    action = player.get_input(go, piece_type)
    
    move += 2
    if piece_type == 1:
        file = open("move_count_black.txt", 'w')
    else:
        file = open("move_count_white.txt", 'w')
    file.write(str(move%24))
    writer.writeOutputFile(action)
    print('Duration:', round(time.time()-s,2))