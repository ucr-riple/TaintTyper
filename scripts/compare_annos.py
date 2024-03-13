import glob
import os
import re
import json
import subprocess
import sys
DEBUG = False
PROJECT_TOTAL_ANNOS=0
PROJECT_TOTAL_ANNOS_T=0
PROJECT_TOTAL_ANNOS_UT=0
PROJECT_TOTAL_ANNOS_PT=0


def collect(directory, res):
    global PROJECT_TOTAL_ANNOS, PROJECT_TOTAL_ANNOS_T, PROJECT_TOTAL_ANNOS_UT,PROJECT_TOTAL_ANNOS_PT
    for dirpath, dirnames, filenames in os.walk(directory):
        for filename in filenames:
            if filename.endswith(".java"):
                filepath = os.path.join(dirpath, filename)
                res[filepath] = find_annos(filepath)
    res['annos_total_count'] = PROJECT_TOTAL_ANNOS
    res['rtainted_annos_total_count'] = PROJECT_TOTAL_ANNOS_T
    res['runtainted_annos_total_count'] = PROJECT_TOTAL_ANNOS_UT
    res['rptainted_annos_total_count'] = PROJECT_TOTAL_ANNOS_PT
    
    return res

def find_annos(filename):
    global PROJECT_TOTAL_ANNOS, PROJECT_TOTAL_ANNOS_T, PROJECT_TOTAL_ANNOS_UT, PROJECT_TOTAL_ANNOS_PT
    rtainted_locations = find_string_locations(filename, "@RTainted")
    rtainted_count = len(rtainted_locations)

    runtainted_locations = find_string_locations(filename, "@RUntainted")
    runtainted_count = len(runtainted_locations)
    
    rptainted_locations = find_string_locations(filename, "@RPolyTainted")
    rptainted_count = len(rptainted_locations)

    total_count = rtainted_count + runtainted_count + rptainted_count
    
    PROJECT_TOTAL_ANNOS = PROJECT_TOTAL_ANNOS + total_count
    PROJECT_TOTAL_ANNOS_T = PROJECT_TOTAL_ANNOS_T + rtainted_count
    PROJECT_TOTAL_ANNOS_UT = PROJECT_TOTAL_ANNOS_UT + runtainted_count
    PROJECT_TOTAL_ANNOS_PT = PROJECT_TOTAL_ANNOS_PT + rptainted_count
    
    return {
        "rtainted_annos": rtainted_locations,
        "rtainted_annos_count": rtainted_count,
        "runtainted_annos": runtainted_locations,
        "runtainted_annos_count": runtainted_count,
        "rptainted_annos": rptainted_locations,
        "rptainted_annos_count": rptainted_count,
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

def compare(PROJECT_NAME, inference, manual):
    os.chdir(os.path.abspath(os.path.dirname(__file__)))
    os.chdir("annotation-comparison")
    os.chdir(PROJECT_NAME)

    project_inf = {}
    project_man = {}
    with open(inference+'.json', 'r', encoding='utf-8', errors='ignore') as f:
        project_inf = json.load(f)
    with open(manual+'.json', 'r', encoding='utf-8', errors='ignore') as f:
        project_man = json.load(f)
        
    res = count_per_file_intersections(project_inf, project_man)
    
    with open(PROJECT_NAME+'_comparison.json', 'w', encoding='utf-8') as f:
                json.dump(res, f, indent=4)
    
def count_per_file_intersections(project_inf, project_man):
    result = {}
    total_annos_common = 0
    total_rtainted_annos_common = 0
    total_runtainted_annos_common = 0
    total_rptainted_annos_common = 0
    files_inf = []
    files_man = []
    
    for file_path in project_inf.keys():
        if file_path.endswith('.java'):
            rtainted_annos_inf = project_inf[file_path]['rtainted_annos']
            runtainted_annos_inf = project_inf[file_path]['runtainted_annos']
            rptainted_annos_inf = project_inf[file_path]['rptainted_annos']
            
            rtainted_annos_man = project_man[file_path]['rtainted_annos']
            runtainted_annos_man = project_man[file_path]['runtainted_annos']
            rptainted_annos_man = project_man[file_path]['rptainted_annos']
            
            runtainted_intersection_set = set(map(tuple, runtainted_annos_inf)) & set(map(tuple, runtainted_annos_man))
            runtainted_intersection_list = [list(item) for item in runtainted_intersection_set]
            total_runtainted_annos_common = total_runtainted_annos_common + len(runtainted_intersection_list)
            
            rtainted_intersection_set = set(map(tuple, rtainted_annos_inf)) & set(map(tuple, rtainted_annos_man))
            rtainted_intersection_list = [list(item) for item in rtainted_intersection_set]
            total_rtainted_annos_common = total_rtainted_annos_common + len(rtainted_intersection_list)
            
            rptainted_intersection_set = set(map(tuple, rptainted_annos_inf)) & set(map(tuple, rptainted_annos_man))
            rptainted_intersection_list = [list(item) for item in rptainted_intersection_set]
            total_rptainted_annos_common = total_rptainted_annos_common + len(rptainted_intersection_list)
            
            total_annos_common = total_annos_common + len(runtainted_intersection_list) + len(rtainted_intersection_list) + len(rptainted_intersection_list)
            
            if len(rtainted_annos_inf) > 0 or len(runtainted_annos_inf) > 0 or len(rtainted_annos_man) > 0 or len(runtainted_annos_man) > 0 or len(rptainted_annos_inf) > 0 or len(rptainted_annos_man) > 0:
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
                    
                    'rptainted_annos_inference': rptainted_annos_inf,
                    'rptainted_annos_human': rptainted_annos_man,
                    'rptainted_annos_common': rptainted_intersection_list,
                    'rptainted_annos_inference_count': len(rptainted_annos_inf),
                    'rptainted_annos_human_count': len(rptainted_annos_man),
                    'rptainted_annos_common_count': len(rptainted_intersection_list),                    
                    
                    
                    "annos_total_count_inference": project_inf[file_path]['total_annos_count'],
                    "annos_total_count_human": project_man[file_path]['total_annos_count'],
                    'annos_common_count':len(rtainted_intersection_list) + len(runtainted_intersection_list)
                }
                
                if len(rtainted_annos_inf) > 0 or len(runtainted_annos_inf) > 0 or len(rptainted_annos_inf) > 0:
                    files_inf.append(file_path)
                if len(rtainted_annos_man) > 0 or len(runtainted_annos_man) > 0 or len(rptainted_annos_man) > 0:
                    files_man.append(file_path)
                    
    files_annotated_common_set = set(files_inf) & set(files_man)            
    result['annos_total_count_inference'] = project_inf['annos_total_count']
    result['rtainted_annos_total_count_inference'] = project_inf['rtainted_annos_total_count']
    result['runtainted_annos_total_count_inference'] = project_inf['runtainted_annos_total_count']
    result['rptainted_annos_total_count_inference'] = project_inf['rptainted_annos_total_count']
    
    result['annos_total_count_human'] = project_man['annos_total_count']
    result['rtainted_annos_total_count_human'] = project_man['rtainted_annos_total_count']
    result['runtainted_annos_total_count_human'] = project_man['runtainted_annos_total_count']
    result['rtainted_annos_total_count_human'] = project_man['rtainted_annos_total_count']
    
    result['annos_total_count_common'] = total_annos_common
    result['annos_total_rtainted_count_common'] = total_rtainted_annos_common
    result['annos_total_rutainted_count_common'] = total_runtainted_annos_common
    result['annos_total_rptainted_count_common'] = total_rptainted_annos_common
    if DEBUG:
        result['files_annotated_inference'] = files_inf
        result['files_annotated_human'] = files_man
        result['files_annotated_common'] = list(files_annotated_common_set)
    result['files_annotated_count_inference'] = len(files_inf)
    result['files_annotated_count_human'] = len(files_man)
    result['files_annotated_count_common'] = len(files_annotated_common_set)
    result['inf_annotation_per_100_lines'] = project_inf['annotations_per_100_lines']
    result['man_annotation_per_100_lines'] = project_man['annotations_per_100_lines']
            
    return result

human_annotated_benchmark = {
    "esapi-java-legacy": ["/Users/kknock/RIPLE/taint/summer/annotated-code/esapi-java-legacy/src/main/java/","annotator-manual"],
    "pybbs": ["/Users/kknock/RIPLE/taint/summer/pybbs/src/main/java", "annotator-manual"],
    "alfresco-community-core": ["/Users/kknock/RIPLE/taint/summer/alfresco-community-repo/core/src/main/java", "annotator-manual-core",],
    "alfresco-community-remote-api": ["/Users/kknock/RIPLE/taint/summer/alfresco-community-repo/remote-api/src/main/java", "annotator-manual-remote-api"],
    "cxf": ["/Users/kknock/RIPLE/taint/summer/cxf/core/src/main/java", "annotator-manual"],
}
annotator_benchmark = {
    "esapi-java-legacy": ["/Users/kknock/RIPLE/taint/summer/annotated-code/esapi-java-legacy/src/main/java/", "annotationsfeb12"],
    "pybbs": ["/Users/kknock/RIPLE/taint/summer/pybbs/src/main/java", "annotations-feb12"],
    "alfresco-community-core": ["/Users/kknock/RIPLE/taint/summer/alfresco-community-repo/core/src/main/java", "annotations-feb12-core"],
    "alfresco-community-remote-api": ["/Users/kknock/RIPLE/taint/summer/alfresco-community-repo/remote-api/src/main/java", "annotations-feb27-remote-api"],
    "commons-configuration": ["/Users/kknock/RIPLE/taint/summer/commons-configuration/src/main/java", "annotationsfeb12"],
    "cxf": ["/Users/kknock/RIPLE/taint/summer/cxf/core/src/main/java", "annotations-feb12"],
    "struts-core": ["/Users/kknock/RIPLE/taint/summer/struts/core/src/main/java", "annotations-feb12-core"],
}

comparison_benchmark = ["esapi-java-legacy", "pybbs", "alfresco-community-core", "alfresco-community-remote-api", "cxf"]
# debug_benchmark = ["pybbs"]

def collect_benchmarks(benchmark):
    global PROJECT_TOTAL_ANNOS, PROJECT_TOTAL_ANNOS_T, PROJECT_TOTAL_ANNOS_UT,PROJECT_TOTAL_ANNOS_PT
    for name, values in benchmark.items():
        PROJECT_TOTAL_ANNOS=0
        PROJECT_TOTAL_ANNOS_T=0
        PROJECT_TOTAL_ANNOS_UT=0
        PROJECT_TOTAL_ANNOS_PT=0
        res = {}
        print(f"Collecting info for {name} on branch {values[1]}...")
        os.chdir(values[0])
        subprocess.run(["git", "reset", "--hard"], check=True)
        subprocess.run(["git", "checkout", values[1]], check=True)
    
        res = collect(values[0], res)
        
        # Count the non-blank lines of Java code
        java_files = glob.glob(os.path.join(values[0], '**/*.java'), recursive=True)
        non_blank_non_comment_line_count = 0
        for java_file in java_files:
            with open(java_file, 'r') as file:
                lines = file.readlines()
            in_multiline_comment = False
            for line in lines:
                stripped_line = line.strip()
                if not stripped_line:
                    continue  # Ignore blank lines
                if stripped_line.startswith('/*'):
                    in_multiline_comment = True
                if not in_multiline_comment and not stripped_line.startswith('//'):
                    non_blank_non_comment_line_count += 1
                if stripped_line.endswith('*/'):
                    in_multiline_comment = False

        # Calculate the annotations per 100 lines of code
        annotations_per_100_lines = round((res["annos_total_count"] / non_blank_non_comment_line_count) * 100 if non_blank_non_comment_line_count else 0, 2)

        # Add these values to the res dictionary
        res['loc'] = non_blank_non_comment_line_count
        res['annotations_per_100_lines'] = annotations_per_100_lines
        
        os.chdir(os.path.abspath(os.path.dirname(__file__)))
        os.chdir("annotation-comparison")
        try:
            os.mkdir(name)
        except:
            print(name + ' directory exists!')
        os.chdir(name)
        
        with open(values[1]+'.json', 'w', encoding='utf-8') as f:
            json.dump(res, f, indent=4)

def compare_benchmarks(benchmark):
    for name in benchmark:
        compare(name, annotator_benchmark[name][1], human_annotated_benchmark[name][1])
        
collect_benchmarks(human_annotated_benchmark)
collect_benchmarks(annotator_benchmark)

compare_benchmarks(comparison_benchmark)