import json
import os
import subprocess
import time

DEBUG = False

human_annotated_benchmark = {
    "esapi-java-legacy": ["/Users/kknock/RIPLE/taint/summer/annotated-code/esapi-java-legacy","annotator-manual"],
    "pybbs": ["/Users/kknock/RIPLE/taint/summer/pybbs", "annotator-manual"],
    "alfresco-community-core": ["/Users/kknock/RIPLE/taint/summer/alfresco-community-repo", "annotator-manual-core",],
    "alfresco-community-remote-api": ["/Users/kknock/RIPLE/taint/summer/alfresco-community-repo", "annotator-manual-remote-api"],
    "cxf": ["/Users/kknock/RIPLE/taint/summer/cxf", "annotator-manual"],
}

debug_benchmark = {
    "pybbs": ["/Users/kknock/RIPLE/taint/summer/pybbs", "annotator-manual"],
}

annotator_benchmark = {
    "esapi-java-legacy": ["/Users/kknock/RIPLE/taint/summer/annotated-code/esapi-java-legacy", "annotationsfeb12"],
    "pybbs": ["/Users/kknock/RIPLE/taint/summer/pybbs", "annotations-feb12"],
    "alfresco-community-core": ["/Users/kknock/RIPLE/taint/summer/alfresco-community-repo", "annotations-feb12-core"],
    "alfresco-community-remote-api": ["/Users/kknock/RIPLE/taint/summer/alfresco-community-repo", "annotations-feb27-remote-api"],
    "commons-configuration": ["/Users/kknock/RIPLE/taint/summer/commons-configuration", "annotationsfeb12"],
    "cxf": ["/Users/kknock/RIPLE/taint/summer/cxf", "annotations-feb12"],
    "struts-core": ["/Users/kknock/RIPLE/taint/summer/struts", "annotations-feb12-core"],
}

java_versions = {
    "esapi-java-legacy": "17.0.7",
    "pybbs": "17.0.7",
    "alfresco-community-core": "17.0.7",
    "alfresco-community-remote-api": "17.0.7",
    "commons-configuration": "17.0.7",
    "cxf": "17.0.7",
    "struts-core": "17.0.7",
}

results_json = {
    "esapi-java-legacy": "annotator-out/0/errors.json",
    "pybbs": "annotator-out/0/errors.json",
    "alfresco-community-core": "annotator-out/core/0/errors.json",
    "alfresco-community-remote-api": "annotator-out/remote-api/0/errors.json",
    "commons-configuration": "annotator-out/0/errors.json",
    "cxf": "annotator-out/0/errors.json",
    "struts-core": "annotator-out/core/0/errors.json"
}

run_info = []

def run_benchmarks(benchmark):
    for name, values in benchmark.items():
        print(f"Running benchmark for {name} on branch {values[1]}...")
        os.chdir(values[0])
        subprocess.run(["git", "reset", "--hard"], check=True)
        subprocess.run(["git", "checkout", values[1]], check=True)
        
        env = os.environ.copy()
        java_home_output = subprocess.run(["/usr/libexec/java_home", "-v", java_versions[name]], capture_output=True, text=True)
        env["JAVA_HOME"] = java_home_output.stdout.strip()
        
        start_time = time.time()
        subprocess.run(["sh", "./annotator-command.sh"], env=env, capture_output=True, text=True)
        end_time = time.time()
        run_time = round(end_time - start_time, 3)
        
        # Get the full path to the results.json file
        reported_errors_path = os.path.join(values[0], results_json[name])
        reported_erros_count = count_non_blank_lines(reported_errors_path)

        # Get the timestamp of the last modification
        timestamp = os.path.getmtime(reported_errors_path)
    
        # Convert the timestamp to a readable format
        readable_timestamp = time.ctime(timestamp)
        print(f"{readable_timestamp}: Run complete for {name} on branch {values[1]} in: {run_time} seconds.")
        run_info.append({"project": name, "branch": values[1], "timestamp": readable_timestamp, "runtime": run_time, "error_count": reported_erros_count})

def experiment():
    run_benchmarks(human_annotated_benchmark)
    run_benchmarks(annotator_benchmark)

    os.chdir(os.path.dirname(os.path.realpath(__file__)))
    run_info.sort(key=lambda x: x['project'])
    with open("run_info.json", "w") as file:
        json.dump(run_info, file)
        
def count_non_blank_lines(filename):
    with open(filename, 'r') as file:
        lines = file.readlines()
    non_blank_count = sum(1 for line in lines if line.strip())
    return non_blank_count
    
# run_benchmarks(debug_benchmark)
# experiment()