#!/bin/bash

PROJECT_DIR="/workspace"
AGENT_SKILLS_DIR="$PROJECT_DIR/.agents/skills"
CLAUDE_SKILLS_DIR="$PROJECT_DIR/.claude/skills"

# ==== Link Agent Skills For Claude Code ==== #

# Claude Code discovers project skills only under .claude/skills/, so every
# skill in .agents/skills/ is exposed there as a relative symlink. Existing
# entries are kept, which also leaves harness-managed folders untouched.

mkdir -p "$CLAUDE_SKILLS_DIR"

for skill_dir in "$AGENT_SKILLS_DIR"/*/; do
  [ -d "$skill_dir" ] || continue

  skill_name="$(basename "$skill_dir")"
  link_path="$CLAUDE_SKILLS_DIR/$skill_name"

  # -L also catches broken links, which -e alone would miss.
  if [ -e "$link_path" ] || [ -L "$link_path" ]; then
    continue
  fi

  ln -s "../../.agents/skills/$skill_name" "$link_path"
  echo "Linked agent skill: $skill_name"
done
