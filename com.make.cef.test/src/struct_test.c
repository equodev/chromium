/*
 * struct_test.c
 *
 *  Created on: Nov 4, 2016
 *      Author: guille
 */

#include "struct_test.h"

#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>

int fn_int_no_args() {
	printf("#in fn_int_no_args, returning 0\n");
	return 0;
}

char* fn_int_int_char_bool_args(int a1, char* a2, bool a3) {
	printf("#in fn_int_string_args with:\n");
	printf(" a1: %i\n", a1);
	printf(" a2: %s\n", a2);
	printf(" a3: %i\n", a3);

	char *ret = malloc (sizeof (char) * 50);
	sprintf(ret, "%d_%s_%s", a1, a2, (a3 == true) ? "true" : "false");

	return ret;
}

char* fn_int_string_array_args(int argc, char** argv) {
	printf("#in fn_int_string_array_args with:\n");
	printf(" argc: %d\n", argc);

	char *ret = malloc (sizeof (char) * 50);
	ret[0] = '\0';
	for (int i = 0; i < argc; ++i) {
		printf(" argv %i: %s\n", i, argv[i]);
		strcat(ret, argv[i]);
	}

	return ret;
}

typedef struct _int_struct {
	int a1;
	char* a2;
} int_struct;

char* fn_struct_int(struct _int_struct* st) {
	printf("#in fn_struct_int:\n");
	printf(" st.a1: %i\n", st->a1);
	printf(" st.a2: %s\n", st->a2);

	char *ret = malloc (sizeof (char) * 50);
	sprintf(ret, "%d_%s", st->a1, st->a2);

	return ret;
}

typedef struct _string_array_struct {
	int a1;
	char** a2;
} string_array_struct;

char* fn_struct_string_array(struct _string_array_struct* st) {
	printf("#in fn_struct_string_array:\n");
	printf(" st.a1: %i\n", st->a1);

	char *ret = malloc (sizeof (char) * 50);
	ret[0] = '\0';
	for (int i = 0; i < st->a1; ++i) {
		printf(" argv %i: %s\n", i, st->a2[i]);
		strcat(ret, st->a2[i]);
	}

	return ret;
}
