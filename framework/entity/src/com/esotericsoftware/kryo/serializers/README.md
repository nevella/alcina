###
I have a feeling this was an attempt to optimise Kryo - but the real solution ended up being pooling instances in KryoUtil. Should compare with trunk