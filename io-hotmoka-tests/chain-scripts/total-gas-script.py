import sys
import os
from response import Response

CHAIN_PATH = "../chain"
TAG_GAS_CPU = "  gas consumed for CPU execution: "
TAG_GAS_RAM = "  gas consumed for RAM allocation: "
TAG_GAS_STORAGE = "  gas consumed for storage consumption: "
NO_GAS_RESPONSES = ["JarStoreInitialTransactionResponse","GameteCreationTransactionResponse", "InitializationTransactionRespon"] # non hanno segnato il gas perchè sono quelle iniziali

def parse_response_file(id_trans, file):
    responsetype, gas_CPU, gas_RAM, gas_storage = None, None, None, None
    for line in file:
        if not line.startswith("  "):
            responsetype = line[:-2]
        elif line.startswith(TAG_GAS_CPU):
            gas_CPU = int(line[len(TAG_GAS_CPU):-1])
        elif line.startswith(TAG_GAS_RAM):
            gas_RAM = int(line[len(TAG_GAS_RAM):-1])
        elif line.startswith(TAG_GAS_STORAGE):
            gas_storage = int(line[len(TAG_GAS_STORAGE):-1])

    if responsetype in NO_GAS_RESPONSES:
        gas_CPU, gas_RAM, gas_storage = 0, 0, 0

    if None not in [responsetype, gas_CPU, gas_RAM, gas_storage]:
        return Response(id_trans, responsetype, gas_CPU, gas_RAM, gas_storage)
    else:
        print("Error: field not found")
        return None


def parse_chain_blocks():
    block_responses = dict() # {"b0": [resp1, resp2, resp3, ..], "b1": [...], ... }
    block_list = os.listdir(CHAIN_PATH)
    # block_list.sort()
    for block in block_list:
        transaction_list = os.listdir(CHAIN_PATH + "/" + block)
        # transaction_list.sort()
        if block not in block_responses:
            block_responses[block] = list()
        for transaction in transaction_list:
            # request_path = CHAIN_PATH + "/" + block + "/" + transaction + "/request.txt"
            response_path = CHAIN_PATH + "/" + block + "/" + transaction + "/response.txt"
            with open(response_path, 'r') as response_file: # open(request_path, 'r') as request_file, 
                r = parse_response_file(transaction, response_file)
                block_responses[block].append(r)
                # if r != None:
                   # print(r)

    return block_responses


def get_stats(block_responses, from_block):
    stats = { 
        "#blocks": 0,
        "#transations": 0,
        "transations_errcount": 0, 
        "gas_cpu": 0, 
        "gas_ram": 0, 
        "gas_storage": 0
        }
        
    if from_block is not None:
    	print("STATISTICS FROM BLOCK:: ", from_block)

    for block_key in block_responses:
        if from_block is None or int(block_key[1:]) >= from_block:
            stats["#blocks"] += 1
            for response in block_responses[block_key]:
                stats["#transations"] += 1
                if response == None:
                    stats["transations_errcount"] += 1
                else:
                    stats["gas_cpu"] += response.gas_CPU
                    stats["gas_ram"] += response.gas_RAM
                    stats["gas_storage"] += response.gas_storage
    
    return stats


def main():
    from_block = None
    if len(sys.argv) == 2:
        from_block = int(sys.argv[1])
    block_responses = parse_chain_blocks()
    print(get_stats(block_responses, from_block))


if __name__ == "__main__":
    main()
