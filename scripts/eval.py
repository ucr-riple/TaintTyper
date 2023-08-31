import os
import re
import json
import sys

PROJECT_TOTAL_ANNOS=0
PROJECT_TOTAL_ANNOS_T=0
PROJECT_TOTAL_ANNOS_UT=0

def collect(directory, res):
    global PROJECT_TOTAL_ANNOS, PROJECT_TOTAL_ANNOS_T, PROJECT_TOTAL_ANNOS_UT
    for dirpath, dirnames, filenames in os.walk(directory):
        for filename in filenames:
            if filename.endswith(".java"):
                filepath = os.path.join(dirpath, filename)
                res[filepath] = find_annos(filepath)
    res['annos_total_count'] = PROJECT_TOTAL_ANNOS
    res['rtainted_annos_total_count'] = PROJECT_TOTAL_ANNOS_T
    res['runtainted_annos_total_count'] = PROJECT_TOTAL_ANNOS_UT
    
    return res

def find_annos(filename):
    global PROJECT_TOTAL_ANNOS, PROJECT_TOTAL_ANNOS_T, PROJECT_TOTAL_ANNOS_UT
    rtainted_locations = find_string_locations(filename, "@RTainted")
    rtainted_count = len(rtainted_locations)

    runtainted_locations = find_string_locations(filename, "@RUntainted")
    runtainted_count = len(runtainted_locations)

    total_count = rtainted_count + runtainted_count
    
    PROJECT_TOTAL_ANNOS = PROJECT_TOTAL_ANNOS + total_count
    PROJECT_TOTAL_ANNOS_T = PROJECT_TOTAL_ANNOS_T + rtainted_count
    PROJECT_TOTAL_ANNOS_UT = PROJECT_TOTAL_ANNOS_UT + runtainted_count
    
    return {
        "rtainted_annos": rtainted_locations,
        "rtainted_annos_count": rtainted_count,
        "runtainted_annos": runtainted_locations,
        "runtainted_annos_count": runtainted_count,
        "total_annos_count": total_count
    }
    
def get_string_matches_in_file(file_path, target):
    list_of_match_locations = []
    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
        file = f.read()
        for match in re.finditer(target, file):
            list_of_match_locations.append([match.start(), match.end()])
        return list_of_match_locations
        
def find_string_locations(file_path, target_string):
    locations = []
    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
        for line_number, line in enumerate(f, 1):
            col = 0
            while target_string in line[col:]:
                col = line.find(target_string, col) + 1
                if col:
                    locations.append((line_number, col))
                else:
                    break
    return locations

def compare(PROJECT_NAME):
    os.chdir(os.path.abspath(os.path.dirname(__file__)))
    try:
        os.chdir(PROJECT_NAME)
    except:
        print(PROJECT_NAME + ' directory does not exist!')
    
    project_inf = {}
    project_man = {}
    with open(PROJECT_NAME+'_inference.json', 'r', encoding='utf-8', errors='ignore') as f:
        project_inf = json.load(f)
    with open(PROJECT_NAME+'_manual.json', 'r', encoding='utf-8', errors='ignore') as f:
        project_man = json.load(f)
        
    res = count_per_file_intersections(project_inf, project_man)
    
    with open(PROJECT_NAME+'_comparison.json', 'w', encoding='utf-8') as f:
                json.dump(res, f, indent=4)
    
def count_per_file_intersections(project_inf, project_man):
    result = {}
    total_annos_common = 0
    files_inf = []
    files_man = []
    
    for file_path in project_inf.keys():
        if file_path.endswith('.java'):
            rtainted_annos_inf = project_inf[file_path]['rtainted_annos']
            runtainted_annos_inf = project_inf[file_path]['runtainted_annos']
            
            rtainted_annos_man = project_man[file_path]['rtainted_annos']
            runtainted_annos_man = project_man[file_path]['runtainted_annos']
            
            runtainted_intersection_set = set(map(tuple, runtainted_annos_inf)) & set(map(tuple, runtainted_annos_man))
            runtainted_intersection_list = [list(item) for item in runtainted_intersection_set]   
            
            rtainted_intersection_set = set(map(tuple, rtainted_annos_inf)) & set(map(tuple, rtainted_annos_man))
            rtainted_intersection_list = [list(item) for item in rtainted_intersection_set]
            
            total_annos_common = total_annos_common + len(runtainted_intersection_list) + len(rtainted_intersection_list)
            
            if len(rtainted_annos_inf) > 0 or len(runtainted_annos_inf) > 0 or len(rtainted_annos_man) > 0 or len(runtainted_annos_man) > 0:
                result[file_path] = {
                    'rtainted_annos_inference': rtainted_annos_inf,
                    'rtainted_annos_human': rtainted_annos_man,
                    'rtainted_annos_common': rtainted_intersection_list,
                    'rtainted_annos_inference_count': len(rtainted_annos_inf),
                    'rtainted_annos_human_count': len(rtainted_annos_man),
                    'rtainted_annos_common_count': len(rtainted_intersection_list),
                    
                    'runtainted_annos_inference': runtainted_annos_inf,
                    'runtainted_annos_human': runtainted_annos_man,
                    'runtainted_annos_common': runtainted_intersection_list,
                    'runtainted_annos_inference_count': len(runtainted_annos_inf),
                    'runtainted_annos_human_count': len(runtainted_annos_man),
                    'runtainted_annos_common_count': len(runtainted_intersection_list),
                    
                    "annos_total_count_inference": project_inf[file_path]['total_annos_count'],
                    "annos_total_count_human": project_man[file_path]['total_annos_count'],
                    'annos_common_count':len(rtainted_intersection_list) + len(runtainted_intersection_list)
                }
                
                if len(rtainted_annos_inf) > 0 or len(runtainted_annos_inf) > 0:
                    files_inf.append(file_path)
                if len(rtainted_annos_man) > 0 or len(runtainted_annos_man) > 0:
                    files_man.append(file_path)
                
    result['annos_total_count_inference'] = project_inf['annos_total_count']
    result['rtainted_annos_total_count_inference'] = project_inf['rtainted_annos_total_count']
    result['runtainted_annos_total_count_inference'] = project_inf['runtainted_annos_total_count']
    
    result['annos_total_count_human'] = project_man['annos_total_count']
    result['rtainted_annos_total_count_human'] = project_man['rtainted_annos_total_count']
    result['runtainted_annos_total_count_human'] = project_man['runtainted_annos_total_count']
    
    result['annos_total_count_common'] = total_annos_common
    result['files_annotated_inference'] = files_inf
    result['files_annotated_count_inference'] = len(files_inf)
    result['files_annotated_human'] = files_man
    result['files_annotated_count_human'] = len(files_man)
    
    files_annotated_common_set = set(files_inf) & set(files_man)
    result['files_annotated_common'] = list(files_annotated_common_set)
    result['files_annotated_count_common'] = len(files_annotated_common_set)
            
    return result
            
            
def print_usage():
    print('Usage: ' + sys.argv[0])
    print('--collect ' + '<project_path> ' + '<annotation_type(inference/manual)>')
    print('--compare ' + '<project_name>')
    
if __name__ == '__main__':
    if len(sys.argv) < 3 or len(sys.argv) > 4:
        print_usage()
    else:
        option = sys.argv[1]
        if option == '--collect':
            PROJECT_DIR = sys.argv[2]
            PROJECT_NAME = PROJECT_DIR.split('/')[-1]
            annotation_type = sys.argv[3]
            res = {}
            res = collect(PROJECT_DIR, res)
            
            os.chdir(os.path.abspath(os.path.dirname(__file__)))
            try:
                os.mkdir(PROJECT_NAME)
            except:
                print(PROJECT_NAME + ' directory exists!')
            os.chdir(PROJECT_NAME)
            
            with open(PROJECT_NAME+'_'+annotation_type+'.json', 'w', encoding='utf-8') as f:
                json.dump(res, f, indent=4)
        elif option == '--compare':
            PROJECT_NAME = sys.argv[2]
            compare(PROJECT_NAME)
            
            
