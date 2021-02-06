class Response:
    def __init__(self, id_trans, response_type, gas_CPU, gas_RAM, gas_storage):
        self.id_trans = id_trans
        self.response_type = response_type
        self.gas_CPU = gas_CPU
        self.gas_RAM = gas_RAM
        self.gas_storage = gas_storage

    def __str__(self):
        return self.id_trans + "::" + self.response_type + "(gas_CPU=" + str(self.gas_CPU) + ", gas_RAM=" + str(self.gas_RAM) + ", gas_storage=" + str(self.gas_storage) + ")"