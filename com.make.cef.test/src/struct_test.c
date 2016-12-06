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

#include <include/internal/cef_string.h>
#include <include/internal/cef_types.h>
#include <include/capi/cef_app_capi.h>

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

typedef struct _string_array_struct_fixed {
	int a1;
	int a2[2];
	char* a3[3];
} string_array_struct_fixed;

typedef struct _string_array_struct {
	int a1;
	int* a2;
	char** a3;
} string_array_struct;

char* fn_struct_string_array_fixed(struct _string_array_struct_fixed* st) {
	printf("#in fn_struct_string_array_fixed:\n");
	printf(" st.a1: %i\n", st->a1);
	printf(" st.a2[0]: %i\n", st->a2[0]);
	printf(" st.a2[1]: %i\n", st->a2[1]);

	char *ret = malloc (sizeof (char) * 50);
	sprintf(ret, "%i:", st->a1);

	for (int i = 0; i < st->a1; ++i) {
		printf(" argv %i: %s\n", i, st->a3[i]);
		strcat(ret, "_");
		strcat(ret, st->a3[i]);
	}

	return ret;
}

char* fn_struct_string_array(struct _string_array_struct* st) {
	printf("#in fn_struct_string_array:\n");
	printf(" st.a1: %i\n", st->a1);
	printf(" st.a2[0]: %i\n", st->a2[0]);
	printf(" st.a2[1]: %i\n", st->a2[1]);

	char *ret = malloc (sizeof (char) * 50);
	sprintf(ret, "%i:", st->a1);

	for (int i = 0; i < st->a1; ++i) {
		printf(" argv %i: %s\n", i, st->a3[i]);
		strcat(ret, "_");
		strcat(ret, st->a3[i]);
	}

	return ret;
}
/*
typedef struct _cef_main_args_t {
  int argc;
  char** argv;
} cef_main_args_t;
*/
char* fn_mainargs(const struct _cef_main_args_t* args, void* windows_sandbox_info) {
	printf("#in fn_mainargs:\n");
	printf(" args.argc: %i\n", args->argc);

	char *ret = malloc (sizeof (char) * 50);
	sprintf(ret, "%i:", args->argc);

	for (int i = 0; i < args->argc; ++i) {
		printf(" argv[%i]: %s\n", i, args->argv[i]);
		strcat(ret, "_");
		strcat(ret, args->argv[i]);
	}

	return ret;
}

char* fn_cefstring8(char* eq, cef_string_utf8_t* fromj) {
	printf("#in fn_cefstring8:\n");
	printf(" eq: %s: %s\n", eq, fromj->str);

	char *ret = malloc (sizeof (char) * 50);
	sprintf(ret, "ok:%i_%ld_%s", fromj != NULL, fromj->length, fromj->str);

	return ret;
}

char* fn_cefstring16(char* eq, cef_string_utf16_t* fromj) {
	printf("#in fn_cefstring16:\n");
	printf(" eq: %s\n", eq);

	char *ret = malloc (sizeof (char) * 50);
	sprintf(ret, "ok:%i_%ld_%s", fromj != NULL, fromj->length, fromj->str);

	return ret;
}

typedef struct _settings_t {
  size_t size;
  int single_process;
  int no_sandbox;
  cef_string_t browser_subprocess_path;
  int multi_threaded_message_loop;
  int windowless_rendering_enabled;
  int command_line_args_disabled;
  cef_string_t cache_path;
  cef_string_t user_data_path;
  int persist_session_cookies;
  int persist_user_preferences;
  cef_string_t user_agent;
  cef_string_t product_version;
  cef_string_t locale;
  cef_string_t log_file;
  //cef_log_severity_t log_severity;
  cef_string_t javascript_flags;
  cef_string_t resources_dir_path;
  cef_string_t locales_dir_path;
  int pack_loading_disabled;
  int remote_debugging_port;
  int uncaught_exception_stack_size;
  int context_safety_implementation;
  int ignore_certificate_errors;
  //cef_color_t background_color;
  cef_string_t accept_language_list;
} settings_t;


typedef struct _cefstring_struct {
	int a1;
	cef_string_utf16_t a2;
	int very_long_field_name;
	cef_string_utf16_t very_long_field_name2;
} cefstring_struct;

char* fn_struct_cefstring(const struct _cefstring_struct* st) {
	printf("#in fn_cefstring_struct:\n");
	printf(" a1: %i\n", st->a1);
	printf(" a2: %s\n", st->a2.str);
	printf(" a3: %i\n", st->very_long_field_name);
	printf(" a3: %s\n", st->very_long_field_name2.str);

	char *ret = malloc (sizeof (char) * 250);
	sprintf(ret, "ok:%i %s %i %s", st->a1, st->a2.str, st->very_long_field_name, st->very_long_field_name2.str);

	return ret;
}

void string_utf8_dtor(char* str) {
	printf("WTF string_utf8_dtor\n");
  //delete [] str;
}

int cef_string_utf8_set(const char* src, size_t src_len,
                                   cef_string_utf8_t* output, int copy) {
  //cef_string_utf8_clear(output);
  if (copy) {
    if (src && src_len > 0) {
      //output->str = new char[src_len+1];
      output->str = malloc (sizeof (char) * src_len+1);
      if (!output->str)
        return 0;

      memcpy(output->str, src, src_len * sizeof(char));
      output->str[src_len] = 0;
      output->length = src_len;
      output->dtor = string_utf8_dtor;
    }
  } else {
    output->str = src;
    output->length = src_len;
    output->dtor = NULL;
  }
  return 1;
}

char* fn_setstring(char* str, cef_string_utf8_t* output) {
	printf("#in fn_setstring\n");
	printf("#in fn_setstring: %li: %s : %li : %p\n", strlen(str), str, sizeof(*output), output);

	printf("#   %i:%s\n", output->length, output->str);

	cef_string_utf8_set(str, strlen(str), output, 1);
	return output->str;
}

char* fn_settings(struct _cef_settings_t* settings, void* windows_sandbox_info) {
	printf("#in fn_settings:\n");
	printf(" settings: %i\n", settings != NULL);
	printf(" browser_subprocess_path: %s\n", settings->browser_subprocess_path.str);
	printf(" size: %li\n", sizeof(*settings));

	cef_settings_t set = {};
	set.resources_dir_path.str = "asd";

	settings->resources_dir_path.str = "from c";
	//cef_string_utf16_set(src, src_len, output, copy)

	char *ret = malloc (sizeof (char) * 250);
	//sprintf(ret, "ok:%i_%i_%s %s", settings->single_process, settings->no_sandbox, settings->browser_subprocess_path /*settings->log_file.str*/, settings->resources_dir_path);
	sprintf(ret, "ok:%li:%li:%i_%i_%s_%s_%s_%i_%i", sizeof(set), sizeof(set.resources_dir_path),
			settings->single_process, settings->no_sandbox,
			settings->browser_subprocess_path.str,
			settings->log_file.str,
			settings->resources_dir_path.str,
			settings->remote_debugging_port,
			settings->log_severity);

	/*for (int i = 0; i < args->argc; ++i) {
		printf(" argv[%i]: %s\n", i, args->argv[i]);
		strcat(ret, "_");
		strcat(ret, args->argv[i]);
	}*/

	return ret;
}

char* fn_app(cef_app_t* application, void* windows_sandbox_info) {
	printf("#in fn_app:\n");
	printf(" application: %i\n", application != NULL);
	printf(" hand: %i\n", application->get_browser_process_handler != NULL);

	if (application->get_browser_process_handler != NULL) {
		printf("calling get_browser_process_handler c > java");
		cef_browser_process_handler_t* bph = application->get_browser_process_handler(application);
		if (bph != NULL) {
			return "ok";
		}
		return "no cef_browser_process_handler_t returned";
	}
	/*char *ret = malloc (sizeof (char) * 50);
	sprintf(ret, "%i:", args->argc);

	for (int i = 0; i < args->argc; ++i) {
		printf(" argv[%i]: %s\n", i, args->argv[i]);
		strcat(ret, "_");
		strcat(ret, args->argv[i]);
	}*/

	return "nok";
}

typedef struct _struct_callback {
	int id;
	void (*callback)();
	int (*callbackArgs)(struct _struct_callback* self, int arg);
} struct_callback;

char* fn_callback(struct_callback* st) {
	printf("#in fn_callback:\n");
	printf(" st.id: %i\n", st->id);
	printf(" st.callback: %i\n", st->callback != NULL);

	char *ret = malloc (sizeof (char) * 10);
	sprintf(ret, "ok_%i", st->id);

	st->callback();

	return ret;
}

char* fn_callback_args(struct_callback* st, int arg) {
	printf("#in fn_callback_args:\n");
	printf(" st.id: %i\n", st->id);
	printf(" st.callback: %i\n", st->callbackArgs != NULL);
	printf(" arg: %i\n", arg);

	char *ret = malloc (sizeof (char) * 50);

	int cbRet = st->callbackArgs(st, arg);

	sprintf(ret, "ok_%i_%i", st->id, cbRet);

	return ret;
}

char* fn_base_refs(cef_base_t* base) {
	printf("#in fn_base_refs:\n");
	printf(" base: %i\n", base != NULL);
	printf(" base.size: %ld\n", base->size);
	//printf(" app.base: %i\n", app->base);
	printf(" base.add_ref: %i\n", base->add_ref != NULL);

	base->add_ref(base);

	return "ok";
}

char* fn_app_refs(cef_app_t* app) {
	printf("#in fn_app_refs:\n");
	printf(" app: %i\n", app != NULL);
	printf(" app.base.size: %ld\n", app->base.size);
	printf(" app.base.add_ref: %i\n", app->base.add_ref != NULL);

	app->base.add_ref(&app->base);

	return "ok";
}
