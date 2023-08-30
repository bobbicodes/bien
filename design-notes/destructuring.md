- I just looked up `clojure.lang.PersistentArrayMap/createAsIfByAssoc` and it's actually not bad. The entire PersistentArrayMap class is not as scary as I thought it would be. I can actually paste it here, in a nested text block
	- ```java
	  static public PersistentArrayMap createAsIfByAssoc(Object[] init){
	  	if ((init.length & 1) == 1)
	                  throw new IllegalArgumentException(String.format("No value supplied for key: %s", init[init.length-1]));
	  	// If this looks like it is doing busy-work, it is because it
	  	// is achieving these goals: O(n^2) run time like
	  	// createWithCheck(), never modify init arg, and only
	  	// allocate memory if there are duplicate keys.
	  	int n = 0;
	  	for(int i=0;i< init.length;i += 2)
	  		{
	  		boolean duplicateKey = false;
	  		for(int j=0;j<i;j += 2)
	  			{
	  			if(equalKey(init[i],init[j]))
	  				{
	  				duplicateKey = true;
	  				break;
	  				}
	  			}
	  		if(!duplicateKey)
	  			n += 2;
	  		}
	  	if(n < init.length)
	  		{
	  		// Create a new shorter array with unique keys, and
	  		// the last value associated with each key.  To behave
	  		// like assoc, the first occurrence of each key must
	  		// be used, since its metadata may be different than
	  		// later equal keys.
	  		Object[] nodups = new Object[n];
	  		int m = 0;
	  		for(int i=0;i< init.length;i += 2)
	  			{
	  			boolean duplicateKey = false;
	  			for(int j=0;j<m;j += 2)
	  				{
	  				if(equalKey(init[i],nodups[j]))
	  					{
	  					duplicateKey = true;
	  					break;
	  					}
	  				}
	  			if(!duplicateKey)
	  				{
	  				int j;
	  				for (j=init.length-2; j>=i; j -= 2)
	  					{
	  					if(equalKey(init[i],init[j]))
	  						{
	  						break;
	  						}
	  					}
	  				nodups[m] = init[i];
	  				nodups[m+1] = init[j+1];
	  				m += 2;
	  				}
	  			}
	  		if (m != n)
	  			throw new IllegalArgumentException("Internal error: m=" + m);
	  		init = nodups;
	  		}
	  	return new PersistentArrayMap(init);
	  }
	  ```
- The entire thing is only 464 lines! Wtf is that
- Here's a complicated destructuring form:
- ```clojure
  (let [m {:j 15 :k 16 :ivec [22 23 24 25]}
        {j :j, k :k, i :i, [r s & t :as v] :ivec, :or {i 12 j 13}} m]
    [i j k r s t v])
  
  -> [12 15 16 22 23 (24 25) [22 23 24 25]]
  ```