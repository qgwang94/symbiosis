package gov.nasa.jpf.symbc;

import java.util.HashSet;
import java.util.Set;

import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DoubleFieldInfo;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.FloatFieldInfo;
import gov.nasa.jpf.jvm.IntegerFieldInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.LongFieldInfo;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.ReferenceFieldInfo;
import gov.nasa.jpf.jvm.StaticArea;
import gov.nasa.jpf.jvm.StaticElementInfo;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.symbc.heap.HeapChoiceGenerator;
import gov.nasa.jpf.symbc.heap.HeapNode;
import gov.nasa.jpf.symbc.heap.Helper;
import gov.nasa.jpf.symbc.heap.SymbolicInputHeap;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.StringSymbolic;

public class JPF_gov_nasa_jpf_symbc_Debug {
	public static PathCondition getPC(MJIEnv env) {
		JVM vm = env.getVM();
		ChoiceGenerator<?> cg = vm.getChoiceGenerator();
		PathCondition pc = null;

		if (!(cg instanceof PCChoiceGenerator)) {
			ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
			while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
				prev_cg = prev_cg.getPreviousChoiceGenerator();
			}
			cg = prev_cg;
		}

		if ((cg instanceof PCChoiceGenerator) && cg != null) {
			pc = ((PCChoiceGenerator) cg).getCurrentPC();
		}
		return pc;
	}

	public static void printPC(MJIEnv env, int objRef, int msgRef) {
		PathCondition pc = getPC(env);
		if (pc != null) {
			//pc.solve();
			System.out.println(env.getStringObject(msgRef) + pc);
		}
		else
			System.out.println(env.getStringObject(msgRef) + " PC is null");
	}

	public static int getSolvedPC(MJIEnv env, int objRef) {
		PathCondition pc = getPC(env);
		if (pc != null) {
			pc.solve();
			return env.newString(pc.toString());
		}
		return env.newString("");
	}



	public static int getSymbolicIntegerValue(MJIEnv env, int objRef, int v) {
		Object [] attrs = env.getArgAttributes();
		
		IntegerExpression sym_arg = (IntegerExpression)attrs[0];
		if (sym_arg !=null)
			return env.newString(sym_arg.toString());
		else
			return env.newString(Integer.toString(v));
	}
    public static int getSymbolicRealValue(MJIEnv env, int objRef, double v) {
    	Object [] attrs = env.getArgAttributes();
		RealExpression sym_arg = (RealExpression)attrs[0];
		if (sym_arg !=null)
			return env.newString(sym_arg.toString());
		else
			return env.newString(Double.toString(v));
    }
    public static int getSymbolicBooleanValue(MJIEnv env, int objRef, boolean v) {
    	Object [] attrs = env.getArgAttributes();
		IntegerExpression sym_arg = (IntegerExpression)attrs[0];
		if (sym_arg !=null)
			return env.newString(sym_arg.toString());
		else
			return env.newString(Boolean.toString(v));
    }

    public static int getSymbolicStringValue(MJIEnv env, int objRef, int stringRef) {
    	Object [] attrs = env.getArgAttributes();
		StringExpression sym_arg = (StringExpression)attrs[0];
		String string_concrete = env.getStringObject(stringRef);
		if (sym_arg !=null)
			return env.newString(sym_arg.toString());
		else
			return env.newString(string_concrete);
    }
    public static void assume(MJIEnv env, int objRef, boolean c) {
    	Object [] attrs = env.getArgAttributes();
		IntegerExpression sym_arg = (IntegerExpression)attrs[0];
		assert(sym_arg==null);
		if(!c) {// instruct JPF to backtrack
			SystemState ss = env.getSystemState();
			ss.setIgnored(true);
		}
    }

	public static int makeSymbolicInteger(MJIEnv env, int objRef, int stringRef) {
		String name = env.getStringObject(stringRef);
		env.setReturnAttribute(new SymbolicInteger(name));
		return 0;
	}

	public static double makeSymbolicReal(MJIEnv env, int objRef, int stringRef) {
		String name = env.getStringObject(stringRef);
		env.setReturnAttribute(new SymbolicReal(name));
		return 0.0;
	}

	public static boolean makeSymbolicBoolean(MJIEnv env, int objRef, int stringRef) {
		String name = env.getStringObject(stringRef);
		env.setReturnAttribute(new SymbolicInteger(name,0,1));
		return false;
	}

	public static int makeSymbolicString(MJIEnv env, int objRef, int stringRef) {
		String name = env.getStringObject(stringRef);
		env.setReturnAttribute(new StringSymbolic(name));
		return env.newString("");
	}
	
//	public static int makeSymbolicRef(MJIEnv env, int objRef, int stringRef, int objvRef) {
//		String name = env.getStringObject(stringRef);
//		env.setReturnAttribute(new SymbolicInteger(name,0,1));
//		return objvRef;
//	}

	// the purpose of this method is to set the PCheap to the "eq null" constraint for the input specified w/ stringRef
	public static int makeSymbolicNull(MJIEnv env, int objRef, int stringRef) {

		// introduce a heap choice generator for the element in the heap
		ThreadInfo ti = env.getVM().getCurrentThread();
		SystemState ss = env.getVM().getSystemState();
		ChoiceGenerator<?> cg;

		if (!ti.isFirstStepInsn()) {
			  cg = new HeapChoiceGenerator(1);  //new
			  ss.setNextChoiceGenerator(cg);
			  env.repeatInvocation();
			  return -1;  // not used anyways
		  }
		//else this is what really returns results

		cg = ss.getChoiceGenerator();
		assert (cg instanceof HeapChoiceGenerator) :
			"expected HeapChoiceGenerator, got: " + cg;

		// see if there were more inputs added before
		ChoiceGenerator<?> prevHeapCG = cg.getPreviousChoiceGenerator();
		  while (!((prevHeapCG == null) || (prevHeapCG instanceof HeapChoiceGenerator))) {
			  prevHeapCG = prevHeapCG.getPreviousChoiceGenerator();
		  }

		  PathCondition pcHeap;
		  SymbolicInputHeap symInputHeap;
		  if (prevHeapCG == null){

			  pcHeap = new PathCondition();
			  symInputHeap = new SymbolicInputHeap();
		  }
		  else {
			  pcHeap = ((HeapChoiceGenerator)prevHeapCG).getCurrentPCheap();
			  symInputHeap = ((HeapChoiceGenerator)prevHeapCG).getCurrentSymInputHeap();
		  }

		String name = env.getStringObject(stringRef);
		String refChain = name + "[-1]"; // why is the type used here? should use the name of the field instead

		SymbolicInteger newSymRef = new SymbolicInteger( refChain);


		// create new HeapNode based on above info
		 // update associated symbolic input heap

		pcHeap._addDet(Comparator.EQ, newSymRef, new IntegerConstant(-1));
	    ((HeapChoiceGenerator)cg).setCurrentPCheap(pcHeap);
	    ((HeapChoiceGenerator)cg).setCurrentSymInputHeap(symInputHeap);
	    //System.out.println(">>>>>>>>>>>> initial pcHeap: " + pcHeap.toString());
		return -1;
	}



	// makes the fields of object referenced by objvRef symbolic
	// moreover it adds this object to the symbolic heap to participate in lazy initialization
	// -- useful for debugging


	public static void makeFieldsSymbolic(MJIEnv env, int objRef, int stringRef, int objvRef) {
		// makes all the fields of obj v symbolic and adds obj v to the symbolic heap to kick off lazy initialization
		if (objvRef == -1)
			throw new RuntimeException("## Error: null object");
		// introduce a heap choice generator for the element in the heap
		ThreadInfo ti = env.getVM().getCurrentThread();
		SystemState ss = env.getVM().getSystemState();
		ChoiceGenerator<?> cg;

		if (!ti.isFirstStepInsn()) {
			  cg = new HeapChoiceGenerator(1);  //new
			  ss.setNextChoiceGenerator(cg);
			  env.repeatInvocation();
			  return;  // not used anyways
		  }
		//else this is what really returns results

		cg = ss.getChoiceGenerator();
		assert (cg instanceof HeapChoiceGenerator) :
			"expected HeapChoiceGenerator, got: " + cg;

		// see if there were more inputs added before
		ChoiceGenerator<?> prevHeapCG = cg.getPreviousChoiceGenerator();
		  while (!((prevHeapCG == null) || (prevHeapCG instanceof HeapChoiceGenerator))) {
			  prevHeapCG = prevHeapCG.getPreviousChoiceGenerator();
		  }

		  PathCondition pcHeap;
		  SymbolicInputHeap symInputHeap;
		  if (prevHeapCG == null){

			  pcHeap = new PathCondition();
			  symInputHeap = new SymbolicInputHeap();
		  }
		  else {
			  pcHeap = ((HeapChoiceGenerator)prevHeapCG).getCurrentPCheap();
			  symInputHeap = ((HeapChoiceGenerator)prevHeapCG).getCurrentSymInputHeap();
		  }


		// set all the fields to be symbolic
		ClassInfo ci = env.getClassInfo(objvRef);
		FieldInfo[] fields = ci.getDeclaredInstanceFields();
		FieldInfo[] staticFields = ci.getDeclaredStaticFields();

		String name = env.getStringObject(stringRef);
		String refChain = name + "["+objvRef+"]"; // why is the type used here? should use the name of the field instead

		SymbolicInteger newSymRef = new SymbolicInteger( refChain);
		//ElementInfo eiRef = DynamicArea.getHeap().get(objvRef);
		ElementInfo eiRef = JVM.getVM().getHeap().get(objvRef);
		Helper.initializeInstanceFields(fields, eiRef, refChain);
		Helper.initializeStaticFields(staticFields, ci, ti);

		// create new HeapNode based on above info
		 // update associated symbolic input heap

        ClassInfo typeClassInfo = eiRef.getClassInfo();


		HeapNode n= new HeapNode(objvRef,typeClassInfo,newSymRef);
		symInputHeap._add(n);
		pcHeap._addDet(Comparator.NE, newSymRef, new IntegerConstant(-1));
	    ((HeapChoiceGenerator)cg).setCurrentPCheap(pcHeap);
	    ((HeapChoiceGenerator)cg).setCurrentSymInputHeap(symInputHeap);
	    //System.out.println(">>>>>>>>>>>> initial pcHeap: " + pcHeap.toString());
		return;
	}

	private static String res;
   	/**
	 * assumes the heap with root n is a tree
	 * should be generalized to graphs - DONE.
	 * not general - do not use this.
	 *
	 */
	static void buildHeapTree(MJIEnv env, int n) {
		res += " { ";
		res += ((n == -1) ? " -1 " : " 0 ");
		if (n != -1) {
			ClassInfo ci = env.getClassInfo(n);
			FieldInfo[] fields = ci.getDeclaredInstanceFields();
			for (int i = 0; i < fields.length; i++)
			if (fields[i] instanceof ReferenceFieldInfo) {
				String fname = fields[i].getName();
				System.out.println("field name " + fname);
				buildHeapTree(env, env.getReferenceField(n, fname));
			}
		}
		res = res + " } ";

	}





	public static void printHeapPC(MJIEnv env, int objRef, int msgRef) {
		// should first solve the pc's!!!!

		JVM vm = env.getVM();
		ChoiceGenerator<?> cg = vm.getChoiceGenerator();
		PathCondition pc = null;

		if (!(cg instanceof HeapChoiceGenerator)) {
			ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
			while (!((prev_cg == null) || (prev_cg instanceof HeapChoiceGenerator))) {
				prev_cg = prev_cg.getPreviousChoiceGenerator();
			}
			cg = prev_cg;
		}

		if ((cg instanceof HeapChoiceGenerator) && cg != null)
			pc = ((HeapChoiceGenerator) cg).getCurrentPCheap();

		if (pc != null)
			System.out.println(env.getStringObject(msgRef) + pc);
		else
			System.out.println(env.getStringObject(msgRef) + "HeapPC is null");
	}

	// the heap shape is captured in the sequence
	private static String sequence;
	// set to keep track of objects already visited; avoids revisiting
	private static HashSet<Integer> discovered;

	private static HashSet<ClassInfo> discoveredClasses; // to keep track of static fields

	// goes through heap rooted in objvRef in DFS order and prints the symbolic heap
	// does not print the static fields
	public static void printSymbolicRef(MJIEnv env, int objRef, int objvRef, int msgRef) {
		discovered = new HashSet<Integer>();
		discoveredClasses = new HashSet<ClassInfo>();
		sequence = "";

		System.out.println("Rooted Heap: \n"+env.getStringObject(msgRef)+toStringSymbolicRef(env,objvRef)+"\n");
	}

	 static String toStringSymbolicRef(MJIEnv env, int objvRef) {
		if (objvRef == -1) {
			sequence += "[";
			sequence += "null";
			sequence += "]";

		}
		else{
			ClassInfo ci = env.getClassInfo(objvRef);
			//ElementInfo ei = DynamicArea.getHeap().get(objvRef);
			ElementInfo ei = JVM.getVM().getHeap().get(objvRef);
			sequence += "["+objvRef+"]";
			if (!discovered.contains(new Integer(objvRef))){
				discovered.add(new Integer(objvRef));
				sequence += "{";

				FieldInfo[] fields = ci.getDeclaredInstanceFields();

				for (int i = 0; i < fields.length; i++) {
					FieldInfo fi = fields[i];
				    String fname = fi.getName();
				    Object attr = ei.getFieldAttr(fi);

					if (fi instanceof ReferenceFieldInfo) {
						//System.out.println("field name " + fname);

						sequence += fname + ":";
						int ref = env.getReferenceField(objvRef, fname);
						// check if field is symbolic

						if(attr!=null) // we reached a symbolic heap node
							sequence += "*";
						else
							toStringSymbolicRef(env, ref);
					}
					else {

						//System.out.println("field name " + fname);

						if(attr!=null) // we reached a symbolic primitive field
							sequence += fname+":"+ (Expression)attr + " ";
						else {
							sequence += fname + ":"+ fi.valueToString(ei.getFields()) + " ";
							// etc: use FieldInfo.valueToString(fields)
						}
					}

				}
				sequence += "}";
			}


			FieldInfo[] staticFields = ci.getDeclaredStaticFields();
			if(staticFields != null && staticFields.length>0) {

				if (!discoveredClasses.contains(ci)){
					sequence += "\n STATICS:";
					discoveredClasses.add(ci);
					sequence += "{";

					for (int i = 0; i < staticFields.length; i++) {
						FieldInfo fi = staticFields[i];
						String fname = fi.getName();

						StaticElementInfo sei = ci.getStaticElementInfo();
						if (sei == null) {
							ci.registerClass(env.getVM().getCurrentThread());
						}
						Object attr = sei.getFieldAttr(staticFields[i]);

						if (staticFields[i] instanceof ReferenceFieldInfo) {

							//System.out.println("field name " + fname);
							sequence += fname + ":";
							int refStatic = env.getStaticReferenceField(ci.getName(), fname);

							if(attr!=null) // we reached a symbolic heap node
								sequence += "*";
							else
								toStringSymbolicRef(env, refStatic);
						}

						else {
							if(attr!=null) // we reached a symbolic primitive node
								sequence += fname+":"+ (Expression)attr + " ";
							else
								sequence += fname + ":"+ fi.valueToString(sei.getFields()) + " ";
						}

					}
					sequence += "}";
				}
			}
		}
		return sequence;
	 }




	/**
	 * @author Mithun Acharya
	 */



	/**
	 * Assumes rooted heap.
	 * A rooted heap is a pair <r, h> of a root object r and
	 * a heap h such that all objects in h are reachable from r
	 *
	 * Performs a DFS over objects reachable from the root, recursively.
	 *
	 * Note:
	 * 	In DFS, discovery and finish time of nodes have parenthesis structure.
	 *
	 *
	 */
	private static String traverseRootedHeapAndGetSequence(MJIEnv env, int n){
		// lets call the current vertex v
		if (n==-1) { // vertex v is null
			// for null vertex, discovery and finish time are the same
			// so open and close the bracket all in one place.
			sequence += "{";
			sequence += "-1";
			sequence += "}";
		}
		else{ // vertex v, is not null
			if (!discovered.contains(new Integer(n))){ // vertex v just discovered
				// discovery time for v - so put v into the hashset and open paranthesis
				discovered.add(new Integer(n));
				sequence += "{";
				sequence += "0";

				// Now start traversing all undiscovered successors of v
				ClassInfo ci = env.getClassInfo(n);
				FieldInfo[] fields = ci.getDeclaredInstanceFields();
				for (int i = 0; i < fields.length; i++)
				if (fields[i] instanceof ReferenceFieldInfo) {
					String fname = fields[i].getName();
					//System.out.println("field name " + fname);
					int temp = env.getReferenceField(n, fname);
					// null (short-circuited) OR successor yet undiscovered
					if(temp==-1 || !discovered.contains(new Integer(temp))){
						traverseRootedHeapAndGetSequence(env, temp);
					}
				}
				// All undiscovered successors of v are discovered. We are finished with v.
				// finish time for v - so, close parenthesis.
				sequence += "}";
			}
			else{ // vertex v is already discovered - do nothing
			}
		}
		return sequence;
	}


	/**
	 *
	 * Linearizes the heap.
	 *
	 */
	private static String linearizeRootedHeap(MJIEnv env, int rootRef){
		// create a new map to store ids of visited objects
		// storing is done to avoid revisiting.
		discovered = new HashSet<Integer>();
		// "empty" the sequence
		sequence="";
		// get the sequence for this rooted heap.
		sequence = traverseRootedHeapAndGetSequence(env, rootRef);
		return sequence;
	}


	// Abstraction used
	// Should be made configuration option
	private static final int HEAP_SHAPE_ABSTRACTION = 1;
	private static final int FIELD_ABSTRACTION = 2;
	private static final int OBSERVER_ABSTRACTION = 3;
	private static final int BRANCH_ABSTRACTION = 4;
	// by default, the abstraction is based on heap shape.
	private static final int ABSTRACTION = HEAP_SHAPE_ABSTRACTION;



 	/**
 	 * Abstraction based on heap shape
 	 */
 	private static String getHeapShapeAbstractedState(MJIEnv env, int objvRef){
 		// get the sequence for the rooted heap through heap linearization
		String sequence = linearizeRootedHeap(env, objvRef);
		return sequence;
 	}



 	/**
 	 * sequence is value of the field or combination of fields.
	 * right now, I will include all non-reference fields which are integers.
	 * for now, state is represented by the concatenation of all non-reference integer fields
 	 */
 	private static String getFieldAbstractedState(MJIEnv env, int objvRef){
 		String sequence = "";
 		ClassInfo ci = env.getClassInfo(objvRef);
		FieldInfo[] fields = ci.getDeclaredInstanceFields();
		for (int i = 0; i < fields.length; i++)
		if (!(fields[i] instanceof ReferenceFieldInfo)) {
			String fname = fields[i].getName();
			//System.out.println("field name " + fname);
			String type = fields[i].getType();
			if (type.equals("int")){
				sequence = sequence + env.getIntField(objvRef, fname);
			}
		}
		return sequence;
 	}



 	/**
 	 * Abstraction is based on user-provided observer method.
 	 * Right now, hardcoded implementation wrt isZero() observer method of IncDec.java
 	 * Hence it works only with IncDecDriverAbstraction.java
 	 * Find out how to invoke observer method on the object here!
 	 *
 	 */
 	private static String getObserverAbstractedState(MJIEnv env, int objvRef) {
 		ClassInfo ci = env.getClassInfo(objvRef);
		FieldInfo[] fields = ci.getDeclaredInstanceFields();
		String fname = fields[0].getName(); // should be global
		int global = env.getIntField(objvRef, fname);
 		if(global==0) return "isZero()==True";
 		else return "isZero()==False";
 	}


 	/**
 	 *
 	 * currently, not sure what to do
 	 */
 	private static String getBranchAbstractedState(MJIEnv env, int objvRef){
 		return null;
 	}



 	/**
 	 * Simply gets the abstracted state (as a String sequence)
 	 * depending on user-defined abstraction
 	 */
	private static String getAbstractedState(MJIEnv env, int objvRef){
		// get the abstracted state as a string sequence based on the abstraction
		String abstractedState = null;
		switch(ABSTRACTION){ // the abstraction for the object state machine.
			case HEAP_SHAPE_ABSTRACTION:
				abstractedState = getHeapShapeAbstractedState(env, objvRef);
				break;
			case FIELD_ABSTRACTION:
				abstractedState = getFieldAbstractedState(env, objvRef);
				break;
			case OBSERVER_ABSTRACTION:
				abstractedState = getObserverAbstractedState(env, objvRef);
				break;
			case BRANCH_ABSTRACTION:
				abstractedState = getBranchAbstractedState(env, objvRef);
				break;
			default:
				break;
		}
		return abstractedState;
	}


 	// stores abstract states seen so far
	private static Set<String> abstracStatesSeenSoFar = new HashSet<String>();
	public static final int NEW_STATE=1;
	public static final int OLD_STATE=2;
	/**
	 * Is the state seen before? Update AND return NEW_STATE
	 * if state is new. if state is old, return OLD_STATE
	 */
	public static int checkAndUpdateAbstractStatesSeenSoFar(String state){
		// the contains check is redundant. retained for clarity.
		if (!abstracStatesSeenSoFar.contains(state)) {
			abstracStatesSeenSoFar.add(state);
			return NEW_STATE; // new state
		}
		return OLD_STATE; // state seen before.
	}




	/**
	 *
	 * Performs abstract matching
	 *
	 */
	public static boolean matchAbstractState(MJIEnv env, int objRef, int objvRef) {
		// get the sequence for the abstracted state
		String abstractedState = getAbstractedState(env, objvRef);

		if(checkAndUpdateAbstractStatesSeenSoFar(abstractedState) == NEW_STATE){
			System.out.println("new state");
			return false; // Verify.ignoreIf will not ignore this state.
		}

		System.out.println("old state");
		return true; // Verify.ignoreIf will ignore this state.
	}
}